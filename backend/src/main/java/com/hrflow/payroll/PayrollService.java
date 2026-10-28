package com.hrflow.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.employee.EmployeeRepository;
import com.hrflow.payroll.PayrollCalculationRequest.EmployeeInput;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PayrollService {
    private final PayrollPeriodRepository periods;
    private final EmployeeRepository employees;
    private final PayrollCalculator calculator;
    private final JdbcTemplate jdbc;
    private final ObjectMapper json = new ObjectMapper();

    public PayrollService(PayrollPeriodRepository periods, EmployeeRepository employees,
            PayrollCalculator calculator, JdbcTemplate jdbc) {
        this.periods = periods; this.employees = employees; this.calculator = calculator;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> listPeriods() {
        return periods.findAllByOrderByStartDateDesc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PayrollResponse getPeriod(Long id) { return toResponse(find(id)); }

    public PayrollResponse calculate(PayrollCalculationRequest request, Long actorId) {
        PayrollPeriod period = locked(request.periodId());
        if (period.getStatus() != PayrollStatus.DRAFT && period.getStatus() != PayrollStatus.CALCULATED)
            throw BusinessException.conflict("PERIOD_FINAL", "Kỳ lương đã duyệt hoặc khóa");
        if (period.getStartDate().isBefore(LocalDate.of(2026, 7, 1))
                || period.getEndDate().isAfter(LocalDate.of(2026, 12, 31))
                || period.getStartDate().getDayOfMonth() != 1
                || !period.getEndDate().equals(period.getStartDate().withDayOfMonth(period.getStartDate().lengthOfMonth()))
                || period.getPaymentDate() == null || !period.getPaymentDate().isAfter(period.getEndDate())
                || period.getPaymentDate().isAfter(LocalDate.of(2026, 12, 31)))
            throw BusinessException.unprocessable("PAYROLL_POLICY_PERIOD", "Bộ quy tắc này chỉ áp dụng kỳ tháng 07–12/2026 chi trong năm 2026");
        Integer overlaps = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payroll_period
                WHERE id <> ? AND start_date <= ? AND end_date >= ?
                """, Integer.class, period.getId(), period.getEndDate(), period.getStartDate());
        if (overlaps != null && overlaps > 0)
            throw BusinessException.conflict("PAYROLL_PERIOD_OVERLAP", "Kỳ lương chồng ngày với kỳ khác");
        if (request.employees() == null || request.employees().isEmpty())
            throw BusinessException.unprocessable("PAYROLL_INPUT_MISSING", "Cần bảng đầu vào đã đối soát");

        Set<Long> supplied = new HashSet<>();
        for (EmployeeInput input : request.employees()) {
            if (input == null || input.employeeId() == null || !supplied.add(input.employeeId()))
                throw BusinessException.unprocessable("PAYROLL_DUPLICATE_EMPLOYEE", "Nhân viên bị trùng hoặc thiếu ID");
            if (!employees.existsById(input.employeeId()))
                throw new ResourceNotFoundException("Nhân viên", input.employeeId());
        }
        List<Long> expected = jdbc.queryForList("""
                SELECT DISTINCT employee_id FROM contract
                WHERE start_date <= ? AND (end_date IS NULL OR end_date >= ?)
                ORDER BY employee_id
                """, Long.class, period.getEndDate(), period.getStartDate());
        if (!supplied.containsAll(expected))
            throw BusinessException.unprocessable("PAYROLL_EMPLOYEES_MISSING",
                    "Thiếu nhân viên có hợp đồng trong kỳ: " + expected.stream().filter(id -> !supplied.contains(id)).toList());

        // Validate all rows before removing the previous CALCULATED preview. The transaction rolls back on any error.
        List<Calculated> calculated = request.employees().stream()
                .map(input -> new Calculated(input, calculator.calculate(input), snapshot(input))).toList();
        jdbc.update("DELETE FROM payslip WHERE payroll_period_id = ?", period.getId());
        for (Calculated row : calculated) savePayslip(period, row);
        period.setCalculatedBy(actorId);
        period.setStatus(PayrollStatus.CALCULATED);
        period.setUpdatedAt(Instant.now());
        periods.saveAndFlush(period);
        audit(period.getId(), actorId, "CALCULATE");
        return toResponse(period);
    }

    public PayrollResponse approve(Long periodId, Long actorId) {
        PayrollPeriod period = locked(periodId);
        if (period.getStatus() != PayrollStatus.CALCULATED)
            throw BusinessException.conflict("PAYROLL_STATE", "Chỉ duyệt kỳ đã tính");
        if (actorId == null || actorId.equals(period.getCalculatedBy()))
            throw BusinessException.forbidden("SELF_APPROVAL", "Người tính lương không được tự duyệt");
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM payslip WHERE payroll_period_id = ?",
                Integer.class, periodId);
        Integer valid = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payslip p WHERE p.payroll_period_id = ?
                  AND p.source_snapshot IS NOT NULL AND p.policy_version = ?
                  AND p.total_employee_insurance = p.bhxh_amount + p.bhyt_amount + p.bhtn_amount
                  AND p.net_salary = p.gross_salary - p.total_employee_insurance - p.pit_amount
                        - p.other_deductions + p.period_adjustments - p.advance_recovery + p.reimbursements
                  AND p.gross_salary = (SELECT COALESCE(SUM(l.amount), 0) FROM payroll_line l
                        WHERE l.payslip_id = p.payslip_id AND l.category IN ('INCOME','ALLOWANCE','OVERTIME','NIGHT_EXTRA'))
                """, Integer.class, periodId, PayrollCalculator.POLICY_VERSION);
        if (count == null || count == 0 || !count.equals(valid))
            throw BusinessException.conflict("PAYROLL_RECONCILIATION", "Phiếu lương chưa đủ hoặc tổng dòng không khớp");
        period.setStatus(PayrollStatus.APPROVED);
        period.setApprovedBy(actorId);
        period.setApprovedAt(Instant.now());
        period.setUpdatedAt(Instant.now());
        periods.saveAndFlush(period);
        audit(periodId, actorId, "APPROVE");
        return toResponse(period);
    }

    public PayrollResponse reset(Long periodId, Long actorId, String reason) {
        PayrollPeriod period = locked(periodId);
        if (period.getStatus() != PayrollStatus.CALCULATED)
            throw BusinessException.conflict("PAYROLL_STATE", "Chỉ trả bản tính nháp về DRAFT");
        if (reason == null || reason.isBlank())
            throw BusinessException.unprocessable("RESET_REASON_REQUIRED", "Cần lý do sửa nguồn lương");
        jdbc.update("DELETE FROM payslip WHERE payroll_period_id = ?", periodId);
        period.setStatus(PayrollStatus.DRAFT);
        period.setCalculatedBy(null);
        period.setUpdatedAt(Instant.now());
        periods.saveAndFlush(period);
        jdbc.update("""
                INSERT INTO payroll_audit (payroll_period_id, actor_user_id, action, reason, policy_version)
                VALUES (?, ?, 'RESET', ?, ?)
                """, periodId, actorId, reason.trim(), PayrollCalculator.POLICY_VERSION);
        return toResponse(period);
    }

    public PayrollResponse lock(Long periodId, Long actorId) {
        PayrollPeriod period = locked(periodId);
        if (period.getStatus() != PayrollStatus.APPROVED)
            throw BusinessException.conflict("PAYROLL_STATE", "Chỉ khóa kỳ đã duyệt");
        if (actorId == null) throw BusinessException.forbidden("ACTOR_REQUIRED", "Thiếu người khóa kỳ");
        period.setStatus(PayrollStatus.LOCKED);
        period.setLockedAt(Instant.now());
        period.setUpdatedAt(Instant.now());
        periods.saveAndFlush(period);
        audit(periodId, actorId, "LOCK");
        return toResponse(period);
    }

    /** Release only on/after the configured pay date. Locked amounts remain immutable. */
    public PayrollResponse publish(Long periodId, Long actorId) {
        PayrollPeriod period = locked(periodId);
        if (period.getStatus() != PayrollStatus.LOCKED)
            throw BusinessException.conflict("PAYROLL_STATE", "Chỉ phát hành phiếu của kỳ đã khóa");
        if (period.getPaymentDate() == null || LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"))
                .isBefore(period.getPaymentDate()))
            throw BusinessException.conflict("PAYROLL_NOT_DUE", "Chưa đến ngày phát hành phiếu lương");
        int released = jdbc.update("""
                UPDATE payslip SET status = 'ISSUED', issued_at = NOW()
                WHERE payroll_period_id = ? AND status = 'CALCULATED'
                """, periodId);
        if (released > 0) audit(periodId, actorId, "PUBLISH");
        return toResponse(period);
    }

    private void savePayslip(PayrollPeriod period, Calculated row) {
        var result = row.result();
        BigDecimal standardDays = result.standardHours().divide(new BigDecimal("8"), 1, RoundingMode.HALF_UP);
        BigDecimal paidDays = result.paidHours().divide(new BigDecimal("8"), 1, RoundingMode.HALF_UP);
        Long id = jdbc.queryForObject("""
                INSERT INTO payslip (payroll_period_id, employee_id, status, base_salary,
                    standard_working_days, actual_worked_days, gross_salary, bhxh_amount,
                    bhyt_amount, bhtn_amount, total_employee_insurance, personal_deduction,
                    taxable_income, pit_amount, other_deductions, period_adjustments,
                    net_salary, net_entitlement, advance_recovery, reimbursements,
                    standard_hours, paid_hours, source_snapshot, policy_version)
                VALUES (?, ?, 'CALCULATED', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                RETURNING payslip_id
                """, Long.class,
                period.getId(), row.input().employeeId(),
                row.input().salarySegments().getFirst().monthlySalary(), standardDays.intValue(), paidDays,
                result.grossSalary(), result.bhxhAmount(), result.bhytAmount(), result.bhtnAmount(),
                result.bhxhAmount().add(result.bhytAmount()).add(result.bhtnAmount()),
                row.input().tax().method() == PayrollCalculationRequest.TaxMethod.PROGRESSIVE
                        ? new BigDecimal("15500000").add(new BigDecimal("6200000")
                            .multiply(BigDecimal.valueOf(row.input().tax().validDependents())))
                        : BigDecimal.ZERO,
                result.taxableIncome(), result.pitAmount(), result.legalDeductions(),
                row.input().netAdjustment(), result.netSalary(), result.netEntitlement(),
                row.input().advanceRecovery(), row.input().reimbursements(), result.standardHours(),
                result.paidHours(), row.snapshot(), PayrollCalculator.POLICY_VERSION);
        int order = 0;
        for (var line : result.lines()) {
            jdbc.update("""
                    INSERT INTO payroll_line (payslip_id, category, label, amount, calculation_note, sort_order)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, id, line.category(), line.label(), line.amount(), line.note(), ++order);
        }
    }

    private String snapshot(EmployeeInput input) {
        try { return json.writeValueAsString(input); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Không thể lưu snapshot lương", ex); }
    }

    private PayrollPeriod find(Long id) {
        return periods.findById(id).orElseThrow(() -> new ResourceNotFoundException("Kỳ tính lương", id));
    }
    private PayrollPeriod locked(Long id) {
        return periods.findLockedById(id).orElseThrow(() -> new ResourceNotFoundException("Kỳ tính lương", id));
    }
    private void audit(Long periodId, Long actorId, String action) {
        jdbc.update("INSERT INTO payroll_audit (payroll_period_id, actor_user_id, action, policy_version) VALUES (?, ?, ?, ?)",
                periodId, actorId, action, PayrollCalculator.POLICY_VERSION);
    }
    private PayrollResponse toResponse(PayrollPeriod period) {
        List<PayrollResponse.PayrollRecordResponse> records = jdbc.query("""
                SELECT p.payslip_id, p.employee_id, e.employee_code, e.full_name, p.policy_version,
                       p.gross_salary, p.bhxh_amount, p.bhyt_amount,
                       p.bhtn_amount, p.pit_amount, p.other_deductions, p.net_salary
                FROM payslip p JOIN employee e ON e.employee_id = p.employee_id
                WHERE p.payroll_period_id = ? ORDER BY p.employee_id
                """, (rs, n) -> new PayrollResponse.PayrollRecordResponse(
                        rs.getLong("payslip_id"), rs.getLong("employee_id"),
                        rs.getString("employee_code"), rs.getString("full_name"),
                        rs.getString("policy_version"),
                        rs.getBigDecimal("gross_salary"), rs.getBigDecimal("bhxh_amount"),
                        rs.getBigDecimal("bhyt_amount"), rs.getBigDecimal("bhtn_amount"),
                        rs.getBigDecimal("pit_amount"), rs.getBigDecimal("other_deductions"),
                        rs.getBigDecimal("net_salary")), period.getId());
        return new PayrollResponse(period.getId(), period.getPeriodCode(), period.getStartDate(),
                period.getEndDate(), period.getStatus(), records.size(), records);
    }
    private record Calculated(EmployeeInput input, PayrollCalculator.SalaryResult result, String snapshot) { }
}
