package com.hrflow.payroll;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.hrflow.common.audit.AuditService;
import com.hrflow.common.audit.NotificationService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.response.ApiError.FieldDetail;
import com.hrflow.common.util.Json;
import com.hrflow.common.util.Sql;
import com.hrflow.common.util.Vn;
import com.hrflow.payroll.ParameterService.Param;
import com.hrflow.payroll.PayrollEngine.Bracket;
import com.hrflow.payroll.PayrollEngine.Input;
import com.hrflow.payroll.PayrollEngine.Item;
import com.hrflow.payroll.PayrollEngine.Params;
import com.hrflow.payroll.PayrollEngine.Result;
import com.hrflow.security.AuthUser;
import com.hrflow.security.PayrollAccess;
import com.hrflow.security.Principals;

/**
 * Kỳ lương DRAFT → CALCULATED → APPROVED → LOCKED (payroll_rules Mục 10):
 * chốt công tạo snapshot bất biến; tính lương đọc snapshot + tham số trong DB; đối soát; duyệt/khóa theo grant riêng,
 * tách biệt người lập/người duyệt, chặn xung đột lợi ích; khóa kỳ kích hoạt tạo PDF phiếu lương.
 */
@Service
public class PayrollService {

    private static final String PERIOD_SELECT = """
            SELECT p.id, p.period_year AS year, p.period_month AS month, p.start_date, p.end_date, p.pay_date, p.status, p.payment_status,
                   p.timesheet_closed, p.snapshot_version, p.calc_version, p.closed_at, p.calculated_at, p.reconcile_note, p.reconciled_at,
                   p.conflict_decision_ref, p.conflict_resolved_at, p.approved_at, p.locked_at, p.paid_at, p.payment_ref, p.published_at,
                   p.closed_by, p.calculated_by, p.reconciled_by, p.approved_by, p.locked_by, p.paid_by, p.conflict_resolved_by,
                   (SELECT COUNT(*) FROM payroll_line l WHERE l.period_id = p.id) AS line_count,
                   (SELECT COUNT(*) FROM payroll_line l WHERE l.period_id = p.id AND l.status = 'NEEDS_REVIEW') AS needs_review_count,
                   (SELECT COALESCE(SUM(l.gross), 0) FROM payroll_line l WHERE l.period_id = p.id) AS total_gross,
                   (SELECT COALESCE(SUM(l.payable), 0) FROM payroll_line l WHERE l.period_id = p.id) AS total_payable
            FROM payroll_period p
            """;

    private final JdbcTemplate jdbc;
    private final ParameterService params;
    private final SnapshotBuilder snapshots;
    private final PayrollAccess access;
    private final AuditService audit;
    private final NotificationService notifications;
    private final Json json;
    private final PayslipService payslips;

    public PayrollService(JdbcTemplate jdbc, ParameterService params, SnapshotBuilder snapshots, PayrollAccess access,
                          AuditService audit, NotificationService notifications, Json json, PayslipService payslips) {
        this.jdbc = jdbc;
        this.params = params;
        this.snapshots = snapshots;
        this.access = access;
        this.audit = audit;
        this.notifications = notifications;
        this.json = json;
        this.payslips = payslips;
    }

    // ---------------- helpers ----------------

    private void trail(long periodId, String action, String from, String to, String detail) {
        AuthUser u = Principals.currentOrNull();
        jdbc.update("""
                INSERT INTO payroll_audit(period_id, action, actor_account_id, actor_name, from_status, to_status, detail)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, periodId, action, u == null ? null : u.userId(), u == null ? null : Principals.displayName(jdbc, u.userId()),
                from, to, detail);
    }

    private Map<String, Object> lockPeriod(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM payroll_period WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy kỳ lương");
        }
        return rows.get(0);
    }

    private static Long asLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }

    private static LocalDate date(Object o) {
        return ((java.sql.Date) o).toLocalDate();
    }

    // ---------------- danh sách / chi tiết ----------------

    private Map<String, Object> view(Map<String, Object> row) {
        AuthUser u = Principals.current();
        long id = ((Number) row.get("id")).longValue();
        boolean canTotals = access.hasAny(id, "VIEW", "PREPARE");
        Map<String, Object> m = Sql.camel(row);
        for (String[] f : new String[][] {{"closedBy", "closedByName"}, {"calculatedBy", "calculatedByName"}, {"reconciledBy", "reconciledByName"},
                {"approvedBy", "approvedByName"}, {"lockedBy", "lockedByName"}, {"paidBy", "paidByName"}, {"conflictResolvedBy", "conflictResolvedByName"}}) {
            Object acc = m.remove(f[0]);
            m.put(f[1], acc == null ? null : Principals.displayName(jdbc, ((Number) acc).longValue()));
            if ("calculatedBy".equals(f[0])) {
                m.put("_calculatedBy", acc);
            }
        }
        if (!canTotals) {
            m.put("totalGross", null);
            m.put("totalPayable", null);
        }
        m.put("allowedActions", actions(u, id, m));
        m.remove("_calculatedBy");
        return m;
    }

    private List<String> actions(AuthUser u, long id, Map<String, Object> m) {
        List<String> out = new ArrayList<>();
        String status = (String) m.get("status");
        boolean prepare = access.has("PREPARE", id);
        Long calculator = asLong(m.get("_calculatedBy"));
        boolean closed = Boolean.TRUE.equals(m.get("timesheetClosed"));
        switch (status) {
            case "DRAFT" -> {
                if (prepare) {
                    out.add("PRECHECK");
                    out.add(closed ? "REOPEN_TIMESHEET" : "CLOSE_TIMESHEET");
                    if (closed) {
                        out.add("CALCULATE");
                    }
                    out.add("CONFLICT_RESOLUTION");
                }
            }
            case "CALCULATED" -> {
                if (prepare) {
                    out.add("CALCULATE");
                    out.add("REOPEN");
                    out.add("CONFLICT_RESOLUTION");
                }
                if (access.has("VIEW", id) && m.get("reconciledAt") == null && !u.userId().equals(calculator)) {
                    out.add("RECONCILE");
                }
                if (access.has("APPROVE", id) && !u.userId().equals(calculator)) {
                    out.add("APPROVE");
                }
            }
            case "APPROVED" -> {
                if (access.has("LOCK", id)) {
                    out.add("LOCK");
                }
            }
            default -> {
                if (access.has("LOCK", id) && "UNPAID".equals(m.get("paymentStatus"))) {
                    out.add("MARK_PAID");
                }
            }
        }
        return out;
    }

    public List<Map<String, Object>> listPeriods() {
        Principals.current();
        return jdbc.queryForList(PERIOD_SELECT + " ORDER BY p.period_year DESC, p.period_month DESC").stream().map(this::view).toList();
    }

    public Map<String, Object> getPeriod(long id) {
        Principals.current();
        List<Map<String, Object>> rows = jdbc.queryForList(PERIOD_SELECT + " WHERE p.id = ?", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy kỳ lương");
        }
        return view(rows.get(0));
    }

    // ---------------- tạo kỳ ----------------

    @Transactional
    public Map<String, Object> createPeriod(int year, int month, LocalDate payDate) {
        AuthUser u = access.require("PREPARE", null);
        if (month < 1 || month > 12) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Tháng không hợp lệ");
        }
        LocalDate start = LocalDate.of(year, month, 1);
        LocalDate end = start.plusMonths(1).minusDays(1);
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM payroll_period WHERE period_year = ? AND period_month = ?", Integer.class, year, month);
        if (dup != null && dup > 0) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Kỳ lương tháng " + month + "/" + year + " đã tồn tại");
        }
        LocalDate pay = payDate != null ? payDate : defaultPayDate(end);
        if (pay.isBefore(end)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày chi trả không được trước ngày cuối kỳ");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO payroll_period(period_year, period_month, start_date, end_date, pay_date, status, payment_status, created_by)
                VALUES (?, ?, ?, ?, ?, 'DRAFT', 'UNPAID', ?) RETURNING id
                """, Long.class, year, month, start, end, pay, u.userId());
        trail(id, "CREATE", null, "DRAFT", "Tạo kỳ lương " + month + "/" + year);
        return getPeriod(id);
    }

    /** Ngày 05 tháng sau; nếu rơi vào cuối tuần/lễ thì lùi về ngày làm việc liền trước. */
    LocalDate defaultPayDate(LocalDate periodEnd) {
        LocalDate d = periodEnd.plusDays(5);
        while (d.getDayOfWeek() == DayOfWeek.SATURDAY || d.getDayOfWeek() == DayOfWeek.SUNDAY
                || Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM holiday WHERE holiday_date = ?)", Boolean.class, d))) {
            d = d.minusDays(1);
        }
        return d;
    }

    // ---------------- kiểm tra đầu vào ----------------

    public Map<String, Object> precheck(long id) {
        access.requireAny(id, "PREPARE", "VIEW");
        Map<String, Object> p = Sql.one(jdbc, "Không tìm thấy kỳ lương", "SELECT id, period_year AS year, period_month AS month, start_date, end_date, pay_date FROM payroll_period WHERE id = ?", id);
        LocalDate start = (LocalDate) p.get("startDate");
        LocalDate end = (LocalDate) p.get("endDate");
        LocalDate pay = (LocalDate) p.get("payDate");
        List<Map<String, Object>> issues = new ArrayList<>();
        List<Long> emps = snapshots.employeesOf(start, end);
        if (!Vn.today().isAfter(end)) {
            issues.add(issue("PERIOD", "BLOCKING", (Long) null, null, null, null, "Kỳ chưa kết thúc (" + end + "), chưa thể chốt công"));
        }
        for (Long emp : emps) {
            Input in = snapshots.build(emp, id, ((Number) p.get("year")).intValue(), ((Number) p.get("month")).intValue(), start, end, pay);
            for (String s : in.dataIssues()) {
                boolean warn = s.startsWith("Lưu ý");
                issues.add(issue(s.contains("hợp đồng") ? "CONTRACT" : s.contains("lương cơ bản") ? "SALARY" : "ATTENDANCE", warn ? "WARNING" : "BLOCKING", in, parseDate(s), s));
            }
            if (in.insurance() == null) {
                issues.add(issue("INSURANCE", "BLOCKING", in, null, "Thiếu hồ sơ bảo hiểm"));
            } else if (!"CONFIRMED".equals(in.insurance().status())) {
                issues.add(issue("INSURANCE", "WARNING", in, null, "Hồ sơ bảo hiểm chưa xác nhận (OPEN-03)"));
            }
            if (in.tax() == null) {
                issues.add(issue("TAX", "BLOCKING", in, null, "Thiếu hồ sơ thuế"));
            } else if (!"CONFIRMED".equals(in.tax().status())) {
                issues.add(issue("TAX", "WARNING", in, null, "Hồ sơ thuế chưa xác nhận (OPEN-02)"));
            }
            for (PayrollEngine.AllowanceIn al : in.allowances()) {
                if ("NEEDS_REVIEW".equals(al.taxClass())) {
                    issues.add(issue("ALLOWANCE", "WARNING", in, null, "Phụ cấp " + al.code() + " chưa phân loại thuế (OPEN-04)"));
                }
            }
        }
        int ymYear = ((Number) p.get("year")).intValue();
        int ymMonth = ((Number) p.get("month")).intValue();
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT e.id, e.code, e.full_name, i.source_ref FROM income_item i JOIN employee e ON e.id = i.employee_id
                WHERE i.period_year = ? AND i.period_month = ? AND i.status = 'DRAFT'
                """, ymYear, ymMonth)) {
            issues.add(rowIssue("INCOME", "WARNING", r, "Khoản thu nhập " + r.get("source_ref") + " chưa được xác nhận nên chưa vào lương"));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT e.id, e.code, e.full_name, s.receipt_code FROM sales_revenue s JOIN employee e ON e.id = s.owner_employee_id
                WHERE s.period_year = ? AND s.period_month = ? AND s.status IN ('DRAFT', 'OWNER_CONFIRMED')
                """, ymYear, ymMonth)) {
            issues.add(rowIssue("SALES", "WARNING", r, "Doanh thu " + r.get("receipt_code") + " chưa được xác nhận đủ nên chưa tính hoa hồng/KPI"));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT e.id, e.code, e.full_name FROM attendance_adjustment x JOIN employee e ON e.id = x.employee_id
                WHERE x.status = 'PENDING' AND x.work_date BETWEEN ? AND ?
                """, start, end)) {
            issues.add(rowIssue("ATTENDANCE", "BLOCKING", r, "Còn yêu cầu điều chỉnh công chờ duyệt"));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT e.id, e.code, e.full_name FROM ot_registration o JOIN ot_schedule s ON s.id = o.schedule_id JOIN employee e ON e.id = o.employee_id
                WHERE o.status = 'APPROVED' AND o.actual_status = 'NOT_RECORDED' AND s.work_date BETWEEN ? AND ?
                """, start, end)) {
            issues.add(rowIssue("OT", "BLOCKING", r, "OT đã duyệt nhưng chưa xác nhận giờ thực tế"));
        }
        for (Map<String, Object> r : jdbc.queryForList("""
                SELECT e.id, e.code, e.full_name FROM ot_registration o JOIN ot_schedule s ON s.id = o.schedule_id JOIN employee e ON e.id = o.employee_id
                WHERE o.status = 'APPROVED' AND o.actual_status = 'CONFIRMED' AND NOT o.hr_checked AND s.work_date BETWEEN ? AND ?
                """, start, end)) {
            issues.add(rowIssue("OT", "WARNING", r, "Giờ OT thực tế chưa được HR đối chiếu"));
        }
        try {
            baseParams(end, pay, "REGION_I", null, false, false);
        } catch (BusinessException ex) {
            issues.add(issue("PARAMETER", "BLOCKING", (Long) null, null, null, null, ex.getMessage()));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("periodId", id);
        out.put("employees", emps.size());
        out.put("blocking", issues.stream().filter(i -> "BLOCKING".equals(i.get("level"))).count());
        out.put("warnings", issues.stream().filter(i -> "WARNING".equals(i.get("level"))).count());
        out.put("issues", issues);
        return out;
    }

    private static LocalDate parseDate(String s) {
        try {
            return LocalDate.parse(s.substring(0, 10));
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private Map<String, Object> issue(String category, String level, Input in, LocalDate date, String message) {
        return issue(category, level, in == null ? null : in.employeeId(), in == null ? null : in.employeeCode(), in == null ? null : in.employeeName(), date, message);
    }

    private Map<String, Object> issue(String category, String level, Long empId, String code, String name, LocalDate date, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("category", category);
        m.put("level", level);
        m.put("employeeId", empId);
        m.put("employeeCode", code);
        m.put("employeeName", name);
        m.put("date", date);
        m.put("message", message);
        return m;
    }

    private Map<String, Object> rowIssue(String category, String level, Map<String, Object> r, String message) {
        return issue(category, level, ((Number) r.get("id")).longValue(), (String) r.get("code"), (String) r.get("full_name"), null, message);
    }

    // ---------------- chốt công ----------------

    @Transactional
    public Map<String, Object> closeTimesheet(long id) {
        AuthUser u = access.require("PREPARE", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"DRAFT".equals(p.get("status")) || Boolean.TRUE.equals(p.get("timesheet_closed"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ chốt công được kỳ DRAFT chưa chốt");
        }
        LocalDate start = date(p.get("start_date"));
        LocalDate end = date(p.get("end_date"));
        List<FieldDetail> details = new ArrayList<>();
        if (!Vn.today().isAfter(end)) {
            details.add(new FieldDetail("period", "Kỳ chưa kết thúc (" + end + ")"));
        }
        int version = ((Number) p.get("snapshot_version")).intValue() + 1;
        int year = ((Number) p.get("period_year")).intValue();
        int month = ((Number) p.get("period_month")).intValue();
        List<Input> inputs = new ArrayList<>();
        for (Long emp : snapshots.employeesOf(start, end)) {
            Input in = snapshots.build(emp, id, year, month, start, end, date(p.get("pay_date")));
            in.dataIssues().stream().filter(s -> !s.startsWith("Lưu ý")).forEach(s -> details.add(new FieldDetail(in.employeeCode(), s)));
            inputs.add(in);
        }
        Integer pendingAdj = jdbc.queryForObject("SELECT COUNT(*) FROM attendance_adjustment WHERE status = 'PENDING' AND work_date BETWEEN ? AND ?", Integer.class, start, end);
        if (pendingAdj != null && pendingAdj > 0) {
            details.add(new FieldDetail("attendance", "Còn " + pendingAdj + " yêu cầu điều chỉnh công chờ duyệt"));
        }
        Integer noActual = jdbc.queryForObject("""
                SELECT COUNT(*) FROM ot_registration o JOIN ot_schedule s ON s.id = o.schedule_id
                WHERE o.status = 'APPROVED' AND o.actual_status = 'NOT_RECORDED' AND s.work_date BETWEEN ? AND ?
                """, Integer.class, start, end);
        if (noActual != null && noActual > 0) {
            details.add(new FieldDetail("ot", noActual + " đăng ký OT đã duyệt chưa xác nhận giờ thực tế"));
        }
        if (!details.isEmpty()) {
            throw BusinessException.unprocessable("SOURCE_NOT_READY", "Dữ liệu nguồn chưa sẵn sàng để chốt công", details);
        }
        for (Input in : inputs) {
            jdbc.update("INSERT INTO timesheet_snapshot(period_id, snapshot_version, employee_id, data) VALUES (?, ?, ?, ?::jsonb)",
                    id, version, in.employeeId(), json.write(in));
        }
        jdbc.update("UPDATE payroll_period SET timesheet_closed = TRUE, snapshot_version = ?, closed_by = ?, closed_at = now(), version = version + 1 WHERE id = ?",
                version, u.userId(), id);
        trail(id, "CLOSE_TIMESHEET", "DRAFT", "DRAFT", "Chốt công, snapshot v" + version + ", " + inputs.size() + " nhân viên");
        return getPeriod(id);
    }

    @Transactional
    public Map<String, Object> reopenTimesheet(long id, String reason) {
        access.require("PREPARE", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"DRAFT".equals(p.get("status")) || !Boolean.TRUE.equals(p.get("timesheet_closed"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ mở lại chốt công khi kỳ DRAFT đã chốt");
        }
        jdbc.update("UPDATE payroll_period SET timesheet_closed = FALSE, version = version + 1 WHERE id = ?", id);
        trail(id, "REOPEN_TIMESHEET", "DRAFT", "DRAFT", reason);
        return getPeriod(id);
    }

    // ---------------- tính lương ----------------

    private Param use(Set<String> unverified, boolean mark, String code, LocalDate d, String region) {
        Param p = params.get(code, d, region);
        if (mark && p.needsReview()) {
            unverified.add(code);
        }
        return p;
    }

    /** Tham số dùng chung (không phụ thuộc nhân viên); các nhánh chưa dùng không bị đánh dấu chưa xác nhận. */
    private Params baseParams(LocalDate end, LocalDate pay, String region, PayrollEngine.Input in, boolean flat, boolean nonResident) {
        Set<String> un = new TreeSet<>();
        boolean hasOt = in != null && !in.ot().isEmpty();
        boolean sales = in != null && in.salesEligible();
        List<Bracket> brackets = jdbc.query("""
                SELECT lower_bound, upper_bound, rate FROM tax_bracket WHERE set_code = 'PIT_RESIDENT_2026'
                  AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?) ORDER BY seq
                """, (rs, i) -> new Bracket(rs.getBigDecimal(1), rs.getBigDecimal(2), rs.getBigDecimal(3)), pay, pay);
        if (brackets.isEmpty()) {
            throw BusinessException.unprocessable("PARAMETER_MISSING", "Thiếu biểu thuế lũy tiến có hiệu lực tại " + pay);
        }
        return new Params(
                use(un, hasOt, "OT_RATE_WEEKDAY", end, null).value(), use(un, hasOt, "OT_RATE_WEEKEND", end, null).value(),
                use(un, hasOt, "OT_RATE_HOLIDAY", end, null).value(), use(un, hasOt, "NIGHT_PREMIUM_RATE", end, null).value(),
                use(un, true, "BHXH_EMPLOYEE_RATE", end, null).value(), use(un, true, "BHYT_EMPLOYEE_RATE", end, null).value(),
                use(un, true, "BHTN_EMPLOYEE_RATE", end, null).value(), use(un, true, "INSURANCE_REFERENCE_SALARY", end, null).value(),
                use(un, true, "BHXH_BHYT_CAP_MULTIPLIER", end, null).value(), use(un, true, "BHTN_CAP_MULTIPLIER", end, null).value(),
                use(un, true, "MIN_WAGE_MONTHLY", end, region).value(), use(un, true, "PERSONAL_DEDUCTION", pay, null).value(),
                use(un, true, "DEPENDENT_DEDUCTION", pay, null).value(), use(un, flat, "PIT_FLAT_10_THRESHOLD", pay, null).value(),
                use(un, flat, "PIT_FLAT_10_RATE", pay, null).value(), use(un, nonResident, "PIT_NON_RESIDENT_RATE", pay, null).value(),
                use(un, sales, "SALES_COMMISSION_RATE", end, null).value(), use(un, sales, "SALES_TARGET_MONTHLY", end, null).value(),
                use(un, sales, "SALES_KPI_BONUS", end, null).value(), brackets, un);
    }

    private Params paramsFor(Input in, LocalDate end, LocalDate pay) {
        boolean flat = in.tax() != null && "FLAT_10".equals(in.tax().method());
        boolean nonRes = in.tax() != null && "NON_RESIDENT_20".equals(in.tax().method());
        return baseParams(end, pay, in.wageRegion(), in, flat, nonRes);
    }

    @Transactional
    public Map<String, Object> calculate(long id) {
        AuthUser u = access.require("PREPARE", id);
        Map<String, Object> p = lockPeriod(id);
        String status = (String) p.get("status");
        if (!"DRAFT".equals(status) && !"CALCULATED".equals(status)) {
            throw BusinessException.conflict("PERIOD_LOCKED", "Kỳ đã duyệt/khóa, không được tính lại");
        }
        if (!Boolean.TRUE.equals(p.get("timesheet_closed"))) {
            throw BusinessException.unprocessable("TIMESHEET_NOT_CLOSED", "Phải chốt công trước khi tính lương");
        }
        LocalDate end = date(p.get("end_date"));
        LocalDate pay = date(p.get("pay_date"));
        int snapshotVersion = ((Number) p.get("snapshot_version")).intValue();
        int calcVersion = ((Number) p.get("calc_version")).intValue() + 1;
        List<Map<String, Object>> snaps = jdbc.queryForList("SELECT employee_id, data FROM timesheet_snapshot WHERE period_id = ? AND snapshot_version = ? ORDER BY employee_id",
                id, snapshotVersion);
        if (snaps.isEmpty()) {
            throw BusinessException.unprocessable("TIMESHEET_NOT_CLOSED", "Không có snapshot công để tính");
        }
        // Ghi đè trọn bộ trong một giao dịch: lỗi giữa chừng rollback cả batch, chạy lại cho cùng kết quả (idempotent).
        jdbc.update("DELETE FROM payroll_line_item WHERE line_id IN (SELECT id FROM payroll_line WHERE period_id = ?)", id);
        jdbc.update("DELETE FROM payroll_line WHERE period_id = ?", id);
        jdbc.update("UPDATE payroll_adjustment SET applied_period_id = NULL WHERE applied_period_id = ? AND status = 'APPROVED'", id);
        for (Map<String, Object> s : snaps) {
            Input in = json.read(String.valueOf(s.get("data")), Input.class);
            Params pr = paramsFor(in, end, pay);
            Result r = PayrollEngine.calculate(in, pr);
            Long lineId = jdbc.queryForObject("""
                    INSERT INTO payroll_line(period_id, calc_version, employee_id, employee_code, employee_name, department_name, job_title_name,
                           status, issues, h_std, h_paid, base_salary, salary_attendance, ot_pay, allowances, commission, kpi_bonus, bonuses,
                           other_additions, gross_adjustment, gross, non_taxable_income, exempt_income, taxable_income, bhxh, bhyt, bhtn,
                           insurance_total, family_relief, assessable_income, pit, legal_deductions, net_adjustment, net_entitlement,
                           advance_recovery, reimbursements, payable, param_snapshot)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                    RETURNING id
                    """, Long.class, id, calcVersion, in.employeeId(), in.employeeCode(), in.employeeName(), in.departmentName(), in.jobTitleName(),
                    r.status(), r.issues().isEmpty() ? null : String.join("\n", r.issues()), r.hStd(), r.hPaid(), r.baseSalary(),
                    r.salaryAttendance(), r.otPay(), r.allowances(), r.commission(), r.kpiBonus(), r.bonuses(), r.otherAdditions(),
                    r.grossAdjustment(), r.gross(), r.nonTaxable(), r.exempt(), r.taxable(), r.bhxh(), r.bhyt(), r.bhtn(), r.insuranceTotal(),
                    r.familyRelief(), r.assessable(), r.pit(), r.legalDeductions(), r.netAdjustment(), r.netEntitlement(), r.advanceRecovery(),
                    r.reimbursements(), r.payable(), json.write(paramSnapshot(pr, in)));
            for (Item i : r.items()) {
                jdbc.update("INSERT INTO payroll_line_item(line_id, seq, category, code, name, amount_raw, amount, source_ref, rule) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        lineId, i.seq(), i.category(), i.code(), i.name(), i.amountRaw(), i.amount(), i.sourceRef(), i.rule());
            }
            for (PayrollEngine.AdjIn adj : in.adjustments()) {
                jdbc.update("UPDATE payroll_adjustment SET applied_period_id = ? WHERE id = ? AND status = 'APPROVED'", id, adj.id());
            }
        }
        jdbc.update("""
                UPDATE payroll_period SET status = 'CALCULATED', calc_version = ?, calculated_by = ?, calculated_at = now(),
                       reconciled_by = NULL, reconciled_at = NULL, reconcile_note = NULL, version = version + 1 WHERE id = ?
                """, calcVersion, u.userId(), id);
        trail(id, "CALCULATE", status, "CALCULATED", "Tính lương v" + calcVersion + " từ snapshot v" + snapshotVersion + ", " + snaps.size() + " dòng");
        return getPeriod(id);
    }

    private Map<String, Object> paramSnapshot(Params p, Input in) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("policy", "payroll_rules v3.0");
        m.put("params", p);
        m.put("employee", Map.of("wageRegion", in.wageRegion(), "dependents", in.dependents()));
        return m;
    }

    @Transactional
    public Map<String, Object> reopen(long id, String reason) {
        access.require("PREPARE", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"CALCULATED".equals(p.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ mở lại được kỳ CALCULATED (kỳ APPROVED/LOCKED không có đường mở lại)");
        }
        jdbc.update("DELETE FROM payroll_line_item WHERE line_id IN (SELECT id FROM payroll_line WHERE period_id = ?)", id);
        jdbc.update("DELETE FROM payroll_line WHERE period_id = ?", id);
        jdbc.update("UPDATE payroll_adjustment SET applied_period_id = NULL WHERE applied_period_id = ? AND status = 'APPROVED'", id);
        jdbc.update("""
                UPDATE payroll_period SET status = 'DRAFT', timesheet_closed = FALSE, reconciled_by = NULL, reconciled_at = NULL,
                       reconcile_note = NULL, version = version + 1 WHERE id = ?
                """, id);
        trail(id, "REOPEN", "CALCULATED", "DRAFT", reason);
        return getPeriod(id);
    }

    // ---------------- đối soát / xung đột / duyệt / khóa / chi trả ----------------

    @Transactional
    public Map<String, Object> reconcile(long id, String note) {
        AuthUser u = access.require("VIEW", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"CALCULATED".equals(p.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ đối soát được kỳ CALCULATED");
        }
        if (u.userId().equals(asLong(p.get("calculated_by")))) {
            audit.denied("SELF_APPROVAL", "payroll_period", id, "Người tính lương tự đối soát");
            throw BusinessException.forbidden("SELF_RECONCILE_FORBIDDEN", "Người đối soát phải khác người tính lương");
        }
        List<FieldDetail> mismatches = new ArrayList<>();
        int snapCount = ((Number) jdbc.queryForObject("SELECT COUNT(*) FROM timesheet_snapshot WHERE period_id = ? AND snapshot_version = ?", Long.class,
                id, p.get("snapshot_version"))).intValue();
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT * FROM payroll_line WHERE period_id = ?", id);
        if (lines.size() != snapCount) {
            mismatches.add(new FieldDetail("lines", "Số dòng lương (" + lines.size() + ") khác số nhân viên trong snapshot (" + snapCount + ")"));
        }
        for (Map<String, Object> l : lines) {
            BigDecimal gross = sum(l, "salary_attendance", "ot_pay", "allowances", "commission", "kpi_bonus", "bonuses", "other_additions", "gross_adjustment");
            BigDecimal ins = sum(l, "bhxh", "bhyt", "bhtn");
            BigDecimal net = ((BigDecimal) l.get("gross")).subtract((BigDecimal) l.get("insurance_total")).subtract((BigDecimal) l.get("pit"))
                    .subtract((BigDecimal) l.get("legal_deductions")).add((BigDecimal) l.get("net_adjustment"));
            BigDecimal payable = ((BigDecimal) l.get("net_entitlement")).subtract((BigDecimal) l.get("advance_recovery")).add((BigDecimal) l.get("reimbursements"));
            String code = (String) l.get("employee_code");
            if (gross.compareTo((BigDecimal) l.get("gross")) != 0) {
                mismatches.add(new FieldDetail(code, "Gross không bằng tổng các thành phần"));
            }
            if (ins.compareTo((BigDecimal) l.get("insurance_total")) != 0) {
                mismatches.add(new FieldDetail(code, "Tổng bảo hiểm không bằng tổng ba quỹ"));
            }
            if (net.compareTo((BigDecimal) l.get("net_entitlement")) != 0) {
                mismatches.add(new FieldDetail(code, "Net entitlement không khớp công thức"));
            }
            if (payable.compareTo((BigDecimal) l.get("payable")) != 0) {
                mismatches.add(new FieldDetail(code, "Thực chi không khớp công thức"));
            }
        }
        if (!mismatches.isEmpty()) {
            throw BusinessException.unprocessable("RECONCILE_MISMATCH", "Đối soát phát hiện chênh lệch", mismatches);
        }
        jdbc.update("UPDATE payroll_period SET reconciled_by = ?, reconciled_at = now(), reconcile_note = ?, version = version + 1 WHERE id = ?", u.userId(), note, id);
        trail(id, "RECONCILE", "CALCULATED", "CALCULATED", note);
        return getPeriod(id);
    }

    private static BigDecimal sum(Map<String, Object> row, String... cols) {
        BigDecimal t = BigDecimal.ZERO;
        for (String c : cols) {
            t = t.add((BigDecimal) row.get(c));
        }
        return t;
    }

    @Transactional
    public Map<String, Object> conflictResolution(long id, String decisionRef, String authorityConfirmedBy, LocalDate confirmedAt, String note) {
        AuthUser u = access.require("PREPARE", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"DRAFT".equals(p.get("status")) && !"CALCULATED".equals(p.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ ghi nhận quyết định xử lý xung đột khi kỳ chưa duyệt");
        }
        String title = u.employeeId() == null ? null : jdbc.query("SELECT j.code FROM employee e JOIN job_title j ON j.id = e.job_title_id WHERE e.id = ?",
                rs -> rs.next() ? rs.getString(1) : null, u.employeeId());
        if ("DIRECTOR".equals(title)) {
            audit.denied("SELF_APPROVAL", "payroll_period", id, "Giám đốc tự ghi nhận quyết định xử lý xung đột của chính mình");
            throw BusinessException.forbidden("DIRECTOR_CANNOT_RESOLVE", "Giám đốc không được tự ghi nhận quyết định xử lý xung đột");
        }
        if (confirmedAt.isAfter(Vn.today())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày xác nhận không được ở tương lai");
        }
        jdbc.update("""
                UPDATE payroll_period SET conflict_decision_ref = ?, conflict_resolved_by = ?, conflict_resolved_at = now(),
                       conflict_note = ?, version = version + 1 WHERE id = ?
                """, decisionRef, u.userId(), "Xác nhận bởi " + authorityConfirmedBy + " ngày " + confirmedAt + ": " + note, id);
        trail(id, "CONFLICT_RESOLUTION", (String) p.get("status"), (String) p.get("status"), decisionRef + " - " + authorityConfirmedBy);
        audit.success("PAYROLL_CONFLICT_RESOLUTION", "payroll_period", id, note, null, Map.of("decisionRef", decisionRef));
        return getPeriod(id);
    }

    @Transactional
    public Map<String, Object> approve(long id) {
        AuthUser u = access.require("APPROVE", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"CALCULATED".equals(p.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ duyệt được kỳ CALCULATED");
        }
        if (u.userId().equals(asLong(p.get("calculated_by"))) || u.userId().equals(asLong(p.get("closed_by")))) {
            audit.denied("SELF_APPROVAL", "payroll_period", id, "Người lập/chốt bảng lương tự duyệt");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Người lập/chốt bảng lương không được tự duyệt");
        }
        if (p.get("reconciled_at") == null) {
            throw BusinessException.conflict("NOT_RECONCILED", "Kỳ lương chưa được đối soát");
        }
        Integer review = jdbc.queryForObject("SELECT COUNT(*) FROM payroll_line WHERE period_id = ? AND status = 'NEEDS_REVIEW'", Integer.class, id);
        if (review != null && review > 0) {
            throw BusinessException.conflict("LINES_NEED_REVIEW", "Còn " + review + " dòng lương NEEDS_REVIEW chưa xử lý");
        }
        boolean hasDirector = Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM payroll_line l JOIN employee e ON e.id = l.employee_id JOIN job_title j ON j.id = e.job_title_id
                               WHERE l.period_id = ? AND j.code = 'DIRECTOR')
                """, Boolean.class, id));
        boolean ownLine = u.employeeId() != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM payroll_line WHERE period_id = ? AND employee_id = ?)", Boolean.class, id, u.employeeId()));
        if ((hasDirector || ownLine) && (p.get("conflict_decision_ref") == null || p.get("conflict_resolved_at") == null)) {
            audit.denied("CONFLICT_OF_INTEREST", "payroll_period", id, "Kỳ có lương của Giám đốc/người duyệt chưa có quyết định xử lý xung đột (OPEN-05)");
            throw BusinessException.conflict("CONFLICT_OF_INTEREST",
                    "Kỳ lương có phần lương của Giám đốc/người duyệt: cần quyết định xử lý xung đột được HNQ xác nhận trước khi duyệt");
        }
        int n = jdbc.update("UPDATE payroll_period SET status = 'APPROVED', approved_by = ?, approved_at = now(), version = version + 1 WHERE id = ? AND status = 'CALCULATED' AND calc_version = ?",
                u.userId(), id, p.get("calc_version"));
        if (n == 0) {
            throw BusinessException.conflict("VERSION_CONFLICT", "Kỳ lương vừa thay đổi, hãy tải lại");
        }
        jdbc.update("UPDATE payroll_adjustment SET status = 'APPLIED' WHERE applied_period_id = ? AND status = 'APPROVED'", id);
        trail(id, "APPROVE", "CALCULATED", "APPROVED", "calc v" + p.get("calc_version"));
        return getPeriod(id);
    }

    @Transactional
    public Map<String, Object> lock(long id) {
        AuthUser u = access.require("LOCK", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"APPROVED".equals(p.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ khóa được kỳ đã APPROVED");
        }
        jdbc.update("UPDATE payroll_period SET status = 'LOCKED', locked_by = ?, locked_at = now(), version = version + 1 WHERE id = ?", u.userId(), id);
        jdbc.update("""
                INSERT INTO background_job(job_type, payload, dedupe_key, status, run_after) VALUES ('GENERATE_PAYSLIPS', ?::jsonb, ?, 'PENDING', now())
                ON CONFLICT (dedupe_key) DO NOTHING
                """, "{\"periodId\":" + id + "}", "payslips:" + id);
        trail(id, "LOCK", "APPROVED", "LOCKED", "Khóa kỳ; xếp hàng tạo phiếu lương");
        // PDF/thông báo chỉ chạy SAU khi commit khóa; lỗi được retry và không đảo trạng thái kỳ.
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                java.util.concurrent.CompletableFuture.runAsync(payslips::runPendingJobs);
            }
        });
        return getPeriod(id);
    }

    @Transactional
    public Map<String, Object> markPaid(long id, String paymentRef, LocalDate paidDate) {
        AuthUser u = access.require("LOCK", id);
        Map<String, Object> p = lockPeriod(id);
        if (!"LOCKED".equals(p.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ ghi nhận chi trả cho kỳ đã khóa");
        }
        if ("PAID".equals(p.get("payment_status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Kỳ đã được ghi nhận chi trả");
        }
        if (paymentRef == null || paymentRef.isBlank()) {
            throw BusinessException.unprocessable("PAYMENT_REF_REQUIRED", "Bắt buộc có số chứng từ chi trả");
        }
        LocalDate d = paidDate == null ? Vn.today() : paidDate;
        jdbc.update("UPDATE payroll_period SET payment_status = 'PAID', paid_by = ?, paid_at = ?, payment_ref = ? WHERE id = ?",
                u.userId(), Timestamp.from(d.atStartOfDay(Vn.ZONE).toInstant()), paymentRef, id);
        trail(id, "MARK_PAID", "LOCKED", "LOCKED", paymentRef);
        return getPeriod(id);
    }

    // ---------------- bảng lương, audit, xuất ----------------

    public List<Map<String, Object>> lines(long id) {
        access.requireAny(id, "VIEW", "PREPARE", "APPROVE", "LOCK");
        getPeriod(id);
        return Sql.list(jdbc, "SELECT * FROM payroll_line WHERE period_id = ? ORDER BY employee_code", id).stream().map(this::lineView).toList();
    }

    public Map<String, Object> line(long lineId) {
        Map<String, Object> l = Sql.one(jdbc, "Không tìm thấy dòng lương", "SELECT * FROM payroll_line WHERE id = ?", lineId);
        access.requireAny(((Number) l.get("periodId")).longValue(), "VIEW", "PREPARE", "APPROVE", "LOCK");
        Map<String, Object> out = lineView(l);
        out.put("items", Sql.list(jdbc, "SELECT seq, category, code, name, amount_raw, amount, source_ref, rule FROM payroll_line_item WHERE line_id = ? ORDER BY seq", lineId));
        audit.success("VIEW_PAYROLL_LINE", "payroll_line", lineId, null, null, null);
        return out;
    }

    private Map<String, Object> lineView(Map<String, Object> l) {
        Object issues = l.get("issues");
        l.put("issues", issues == null ? List.of() : List.of(((String) issues).split("\n")));
        return l;
    }

    public List<Map<String, Object>> audit(long id) {
        access.requireAny(id, "VIEW", "PREPARE", "APPROVE", "LOCK");
        return Sql.list(jdbc, "SELECT id, action, actor_name, from_status, to_status, detail, created_at FROM payroll_audit WHERE period_id = ? ORDER BY id", id);
    }

    public String exportCsv(long id) {
        access.require("VIEW", id);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM payroll_line WHERE period_id = ? ORDER BY employee_code", id);
        StringBuilder sb = new StringBuilder("employee_code,employee_name,status,gross,insurance_total,pit,net_entitlement,payable\n");
        for (Map<String, Object> r : rows) {
            sb.append(r.get("employee_code")).append(",\"").append(String.valueOf(r.get("employee_name")).replace("\"", "\"\"")).append("\",")
                    .append(r.get("status")).append(',').append(r.get("gross")).append(',').append(r.get("insurance_total")).append(',')
                    .append(r.get("pit")).append(',').append(r.get("net_entitlement")).append(',').append(r.get("payable")).append('\n');
        }
        audit.success("EXPORT_PAYROLL", "payroll_period", id, null, null, Map.of("rows", rows.size()));
        return sb.toString();
    }

    // ---------------- tham số ----------------

    public List<Map<String, Object>> parameters(String category) {
        AuthUser u = Principals.current();
        if (!u.is("HR", "ADMIN") && !access.hasAny(null, "VIEW", "PREPARE", "APPROVE", "LOCK")) {
            throw new AccessDeniedException("Không có quyền xem tham số payroll");
        }
        String sql = "SELECT id, code, name, category, value, unit, region, effective_from, effective_to, source_ref, status, confirmed_by, confirmed_at, param_version AS version FROM payroll_parameter";
        return category == null || category.isBlank() ? Sql.list(jdbc, sql + " ORDER BY code, region NULLS FIRST, effective_from DESC, param_version DESC")
                : Sql.list(jdbc, sql + " WHERE category = ? ORDER BY code, region NULLS FIRST, effective_from DESC, param_version DESC", category);
    }

    public record ParamCmd(String code, String name, String category, BigDecimal value, String unit, String region,
                           LocalDate effectiveFrom, String sourceRef, String status, String confirmedBy) {
    }

    @Transactional
    public Map<String, Object> createParameter(ParamCmd c) {
        Principals.requireRole("HR");
        if (!Set.of("COMPANY", "LEGAL").contains(c.category()) || !Set.of("VERIFIED_SIM", "NEEDS_REVIEW").contains(c.status())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Nhóm/trạng thái tham số không hợp lệ");
        }
        if ("VERIFIED_SIM".equals(c.status()) && (c.confirmedBy() == null || c.confirmedBy().isBlank())) {
            throw BusinessException.unprocessable("CONFIRMER_REQUIRED", "Tham số đã xác nhận phải có người xác nhận");
        }
        LocalDate monthStart = c.effectiveFrom().withDayOfMonth(1);
        Integer frozen = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payroll_period WHERE end_date >= ? AND (timesheet_closed OR status IN ('APPROVED', 'LOCKED'))
                """, Integer.class, c.effectiveFrom());
        if (frozen != null && frozen > 0) {
            throw BusinessException.conflict("PERIOD_FROZEN", "Ngày hiệu lực thuộc/trước kỳ đã chốt công hoặc khóa; thay đổi tham số phải áp dụng cho kỳ sau");
        }
        Integer version = jdbc.queryForObject("SELECT COALESCE(MAX(param_version), 0) + 1 FROM payroll_parameter WHERE code = ? AND COALESCE(region, '-') = COALESCE(?, '-')",
                Integer.class, c.code(), c.region());
        jdbc.update("""
                UPDATE payroll_parameter SET effective_to = ? - 1 WHERE code = ? AND COALESCE(region, '-') = COALESCE(?, '-')
                  AND effective_to IS NULL AND effective_from < ?
                """, c.effectiveFrom(), c.code(), c.region(), c.effectiveFrom());
        Long id = jdbc.queryForObject("""
                INSERT INTO payroll_parameter(code, name, category, value, unit, region, effective_from, source_ref, status, confirmed_by, confirmed_at, param_version)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, c.code(), c.name(), c.category(), c.value(), c.unit(), c.region(), c.effectiveFrom(), c.sourceRef(), c.status(),
                c.confirmedBy(), "VERIFIED_SIM".equals(c.status()) ? Vn.today() : null, version);
        audit.success("CREATE_PARAMETER", "payroll_parameter", id, c.sourceRef(), null, Map.of("code", c.code(), "value", c.value(), "from", c.effectiveFrom().toString(), "monthStart", monthStart.toString()));
        return Sql.one(jdbc, "Không tìm thấy tham số", "SELECT id, code, name, category, value, unit, region, effective_from, effective_to, source_ref, status, confirmed_by, confirmed_at, param_version AS version FROM payroll_parameter WHERE id = ?", id);
    }

    // ---------------- adjustment sau khóa ----------------

    private static final String ADJ_SELECT = """
            SELECT a.id, a.origin_period_id, op.period_month || '/' || op.period_year AS origin_period, a.applied_period_id,
                   CASE WHEN ap.id IS NULL THEN NULL ELSE ap.period_month || '/' || ap.period_year END AS applied_period,
                   a.employee_id, e.code AS employee_code, e.full_name AS employee_name, a.adj_type, a.amount, a.tax_class, a.reason,
                   a.evidence, a.source_ref, a.status, COALESCE(ce.full_name, ca.email) AS created_by_name,
                   COALESCE(ae.full_name, aa.email) AS approved_by_name, a.approved_at, a.decision_note, a.created_at, a.created_by
            FROM payroll_adjustment a JOIN payroll_period op ON op.id = a.origin_period_id LEFT JOIN payroll_period ap ON ap.id = a.applied_period_id
            JOIN employee e ON e.id = a.employee_id JOIN user_account ca ON ca.id = a.created_by LEFT JOIN employee ce ON ce.id = ca.employee_id
            LEFT JOIN user_account aa ON aa.id = a.approved_by LEFT JOIN employee ae ON ae.id = aa.employee_id
            """;

    private Map<String, Object> adjView(Map<String, Object> m) {
        AuthUser u = Principals.current();
        m.put("canApprove", "PENDING".equals(m.get("status")) && access.has("APPROVE", null) && ((Number) m.get("createdBy")).longValue() != u.userId());
        return m;
    }

    public List<Map<String, Object>> adjustments() {
        access.requireAny(null, "VIEW", "PREPARE", "APPROVE");
        return Sql.list(jdbc, ADJ_SELECT + " ORDER BY a.id DESC").stream().map(this::adjView).toList();
    }

    public record AdjCmd(long originPeriodId, long employeeId, String adjType, BigDecimal amount, String taxClass, String reason,
                         String evidence, String sourceRef) {
    }

    @Transactional
    public Map<String, Object> createAdjustment(AdjCmd c) {
        AuthUser u = access.require("PREPARE", null);
        if (!Set.of("GROSS", "TAX", "INSURANCE", "NET").contains(c.adjType()) || c.amount().signum() == 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Loại điều chỉnh không hợp lệ hoặc số tiền bằng 0");
        }
        if (c.reason() == null || c.reason().isBlank() || c.evidence() == null || c.evidence().isBlank()) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Bắt buộc có lý do và minh chứng");
        }
        String status = jdbc.query("SELECT status FROM payroll_period WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, c.originPeriodId());
        if (status == null) {
            throw new ResourceNotFoundException("Không tìm thấy kỳ gốc");
        }
        if (!"LOCKED".equals(status)) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ tạo adjustment cho kỳ đã khóa; kỳ chưa khóa thì sửa nguồn rồi tính lại");
        }
        Integer inPeriod = jdbc.queryForObject("SELECT COUNT(*) FROM payroll_line WHERE period_id = ? AND employee_id = ?", Integer.class, c.originPeriodId(), c.employeeId());
        if (inPeriod == null || inPeriod == 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Nhân viên không có dòng lương trong kỳ gốc");
        }
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM payroll_adjustment WHERE source_ref = ?", Integer.class, c.sourceRef());
        if (dup != null && dup > 0) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Mã nguồn adjustment đã tồn tại (không cộng trùng)");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO payroll_adjustment(origin_period_id, employee_id, adj_type, amount, tax_class, reason, evidence, source_ref, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?) RETURNING id
                """, Long.class, c.originPeriodId(), c.employeeId(), c.adjType(), c.amount(), "GROSS".equals(c.adjType()) ? (c.taxClass() == null ? "NEEDS_REVIEW" : c.taxClass()) : c.taxClass(),
                c.reason(), c.evidence(), c.sourceRef().trim(), u.userId());
        trail(c.originPeriodId(), "ADJUSTMENT_CREATE", status, status, c.sourceRef());
        return adjView(Sql.one(jdbc, "Không tìm thấy adjustment", ADJ_SELECT + " WHERE a.id = ?", id));
    }

    @Transactional
    public Map<String, Object> decideAdjustment(long id, boolean approve, String note) {
        AuthUser u = access.require("APPROVE", null);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM payroll_adjustment WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy adjustment");
        }
        Map<String, Object> a = rows.get(0);
        if (((Number) a.get("created_by")).longValue() == u.userId()) {
            audit.denied("SELF_APPROVAL", "payroll_adjustment", id, "Người tạo tự duyệt adjustment");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Người tạo không được tự duyệt adjustment");
        }
        if (!"PENDING".equals(a.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Adjustment đã được xử lý");
        }
        if (approve && "GROSS".equals(a.get("adj_type")) && "NEEDS_REVIEW".equals(a.get("tax_class"))) {
            throw BusinessException.unprocessable("TAX_CLASS_REQUIRED", "Adjustment Gross phải có phân loại thuế trước khi duyệt");
        }
        if (!approve && (note == null || note.isBlank())) {
            throw BusinessException.unprocessable("REASON_REQUIRED", "Từ chối phải ghi lý do");
        }
        jdbc.update("UPDATE payroll_adjustment SET status = ?, approved_by = ?, approved_at = now(), decision_note = ? WHERE id = ?",
                approve ? "APPROVED" : "REJECTED", u.userId(), note, id);
        audit.success(approve ? "APPROVE_PAYROLL_ADJUSTMENT" : "REJECT_PAYROLL_ADJUSTMENT", "payroll_adjustment", id, note, null, null);
        return adjView(Sql.one(jdbc, "Không tìm thấy adjustment", ADJ_SELECT + " WHERE a.id = ?", id));
    }
}
