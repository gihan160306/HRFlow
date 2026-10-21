
package com.hrflow.payslip;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PayslipService {

    private final JdbcTemplate jdbcTemplate;

    public PayslipService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Lấy danh sách phiếu lương của một nhân viên.
     */
    @Transactional(readOnly = true)
    public List<PayslipResponse> getEmployeePayslips(
            Long employeeId,
            Long currentUserId,
            String role
    ) {
        checkEmployeeAccess(employeeId, currentUserId, role);

        String sql = """
                SELECT p.payslip_id
                FROM payslip p
                JOIN payroll_period pp
                    ON pp.id = p.payroll_period_id
                WHERE p.employee_id = ?
                  AND pp.status IN ('CALCULATED', 'APPROVED', 'LOCKED')
                  AND p.status IN ('CALCULATED', 'FINALIZED', 'LOCKED', 'ISSUED')
                ORDER BY pp.start_date DESC
                """;

        List<Long> ids = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> rs.getLong("payslip_id"),
                employeeId
        );

        return ids.stream()
                .map(id -> getPayslip(id, currentUserId, role))
                .toList();
    }

    /**
     * Lấy chi tiết một phiếu lương.
     */
    @Transactional(readOnly = true)
    public PayslipResponse getPayslip(
            Long payslipId,
            Long currentUserId,
            String role
    ) {
        String sql = """
                SELECT
                    p.payslip_id,
                    p.employee_id,
                    p.payroll_period_id AS period_id,
                    pp.period_code,
                    pp.start_date,
                    pp.end_date,
                    pp.standard_working_days,
                    p.status,
                    p.gross_salary,
                    p.bhxh_amount,
                    p.bhyt_amount,
                    p.bhtn_amount,
                    p.pit_amount,
                    p.other_deductions,
                    p.period_adjustments,
                    p.carried_debt,
                    p.net_salary,
                    p.created_at
                FROM payslip p
                JOIN payroll_period pp
                    ON pp.id = p.payroll_period_id
                WHERE p.payslip_id = ?
                  AND pp.status IN ('CALCULATED', 'APPROVED', 'LOCKED')
                  AND p.status IN ('CALCULATED', 'FINALIZED', 'LOCKED', 'ISSUED')
                """;

        List<PayslipResponse> results = jdbcTemplate.query(
                sql,
                (rs, rowNum) -> mapPayslip(rs),
                payslipId
        );

        if (results.isEmpty()) {
            // Không tiết lộ phiếu lương không tồn tại hay chưa được phát hành.
            throw new IllegalArgumentException(
                    "Không tìm thấy phiếu lương đã phát hành."
            );
        }

        PayslipResponse payslip = results.get(0);

        checkEmployeeAccess(
                payslip.employeeId(),
                currentUserId,
                role
        );

        List<PayslipResponse.PayslipLine> lines =
                getPayslipLines(payslipId);

        return new PayslipResponse(
                payslip.payslipId(),
                payslip.employeeId(),
                payslip.periodId(),
                payslip.periodCode(),
                payslip.startDate(),
                payslip.endDate(),
                payslip.standardWorkingDays(),
                payslip.status(),
                payslip.grossSalary(),
                payslip.bhxhAmount(),
                payslip.bhytAmount(),
                payslip.bhtnAmount(),
                payslip.totalInsurance(),
                payslip.pitAmount(),
                payslip.otherDeductions(),
                payslip.periodAdjustments(),
                payslip.carriedDebt(),
                payslip.netSalary(),
                payslip.createdAt(),
                lines
        );
    }

    /**
     * Lấy chi tiết các khoản đã được Payroll lưu.
     */
    private List<PayslipResponse.PayslipLine> getPayslipLines(
            Long payslipId
    ) {
        String sql = """
                SELECT
                    payroll_line_id,
                    category,
                    label,
                    amount,
                    calculation_note
                FROM payroll_line
                WHERE payslip_id = ?
                ORDER BY sort_order ASC, payroll_line_id ASC
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new PayslipResponse.PayslipLine(
                        rs.getLong("payroll_line_id"),
                        rs.getString("category"),
                        rs.getString("label"),
                        rs.getBigDecimal("amount"),
                        rs.getString("calculation_note")
                ),
                payslipId
        );
    }

    /**
     * Kiểm tra quyền xem phiếu lương.
     */
    private void checkEmployeeAccess(
            Long targetEmployeeId,
            Long currentUserId,
            String role
    ) {
        if (role == null || currentUserId == null) {
            throw new AccessDeniedException(
                    "Không xác định được người dùng hiện tại."
            );
        }

        // HR và Admin được xem theo chính sách phân quyền.
        if ("HR".equalsIgnoreCase(role)
                || "ADMIN".equalsIgnoreCase(role)) {
            return;
        }

        // Lấy employee_id của tài khoản đang đăng nhập.
        List<Long> employeeIds = jdbcTemplate.query(
                """
                SELECT employee_id
                FROM app_user
                WHERE (user_id = ? OR id = ?)
                  AND (active = TRUE OR is_active = TRUE)
                """,
                (rs, rowNum) -> {
                    long id = rs.getLong("employee_id");
                    return rs.wasNull() ? null : id;
                },
                currentUserId,
                currentUserId
        );

        Long currentEmployeeId = employeeIds.stream()
                .filter(id -> id != null)
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "Tài khoản không gắn với hồ sơ nhân viên."
                ));

        // Nhân viên chỉ được xem phiếu lương của mình.
        if ("EMPLOYEE".equalsIgnoreCase(role)) {
            if (!currentEmployeeId.equals(targetEmployeeId)) {
                throw new AccessDeniedException(
                        "Bạn không có quyền xem phiếu lương này."
                );
            }
            return;
        }

        // Manager được xem phiếu lương của bản thân
        // và nhân viên báo cáo trực tiếp cho mình.
        if ("MANAGER".equalsIgnoreCase(role)) {
            if (currentEmployeeId.equals(targetEmployeeId)) {
                return;
            }

            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*)
                    FROM employee
                    WHERE employee_id = ?
                      AND manager_id = ?
                    """,
                    Integer.class,
                    targetEmployeeId,
                    currentEmployeeId
            );

            if (count != null && count > 0) {
                return;
            }
        }

        throw new AccessDeniedException(
                "Bạn không có quyền xem phiếu lương này."
        );
    }

    private PayslipResponse mapPayslip(ResultSet rs)
            throws SQLException {

        BigDecimal bhxh = zeroIfNull(
                rs.getBigDecimal("bhxh_amount")
        );
        BigDecimal bhyt = zeroIfNull(
                rs.getBigDecimal("bhyt_amount")
        );
        BigDecimal bhtn = zeroIfNull(
                rs.getBigDecimal("bhtn_amount")
        );

        return new PayslipResponse(
                rs.getLong("payslip_id"),
                rs.getLong("employee_id"),
                rs.getLong("period_id"),
                rs.getString("period_code"),
                rs.getDate("start_date").toLocalDate(),
                rs.getDate("end_date").toLocalDate(),
                (Integer) rs.getObject("standard_working_days"),
                rs.getString("status"),
                zeroIfNull(rs.getBigDecimal("gross_salary")),
                bhxh,
                bhyt,
                bhtn,
                bhxh.add(bhyt).add(bhtn),
                zeroIfNull(rs.getBigDecimal("pit_amount")),
                zeroIfNull(rs.getBigDecimal("other_deductions")),
                zeroIfNull(rs.getBigDecimal("period_adjustments")),
                zeroIfNull(rs.getBigDecimal("carried_debt")),
                zeroIfNull(rs.getBigDecimal("net_salary")),
                rs.getTimestamp("created_at") == null
                        ? null
                        : rs.getTimestamp("created_at")
                                .toLocalDateTime(),
                List.of()
        );
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
