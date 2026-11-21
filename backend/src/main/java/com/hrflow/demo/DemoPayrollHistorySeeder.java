package com.hrflow.demo;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.hrflow.payroll.PayrollService;
import com.hrflow.payroll.PayrollService.AdjCmd;
import com.hrflow.security.AuthUser;

/**
 * Dữ liệu demo lịch sử lương (V23): đưa các kỳ 08, 09, 10/2026 đi hết quy trình nghiệp vụ THẬT bằng đúng người có
 * grant payroll: HR Lead chốt công + tính lương, HR Executive đối soát, HR Lead ghi nhận quyết định xử lý xung đột,
 * Giám đốc duyệt, Finance Manager khóa kỳ (tạo phiếu lương PDF) và ghi nhận chi trả.
 * Sau khi kỳ 08 khóa, tạo thêm điều chỉnh truy lĩnh/truy thu để kỳ 09 áp dụng.
 *
 * Chỉ chạy một lần theo background_job DEMO_PAYROLL_HISTORY do V23 tạo; tắt bằng hrflow.demo.payroll-history.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "hrflow.demo.payroll-history.enabled", havingValue = "true")
public class DemoPayrollHistorySeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoPayrollHistorySeeder.class);
    private static final String JOB_TYPE = "DEMO_PAYROLL_HISTORY";
    private static final int YEAR = 2026;
    private static final int[] MONTHS = {8, 9, 10};

    private final JdbcTemplate jdbc;
    private final PayrollService payroll;

    public DemoPayrollHistorySeeder(JdbcTemplate jdbc, PayrollService payroll) {
        this.jdbc = jdbc;
        this.payroll = payroll;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<Long> jobs = jdbc.queryForList("""
                SELECT id FROM background_job WHERE job_type = ? AND status IN ('PENDING', 'FAILED') AND attempts < 3 ORDER BY id
                """, Long.class, JOB_TYPE);
        for (Long jobId : jobs) {
            jdbc.update("UPDATE background_job SET status = 'RUNNING', attempts = attempts + 1 WHERE id = ?", jobId);
            try {
                for (int month : MONTHS) {
                    process(month);
                }
                jdbc.update("UPDATE background_job SET status = 'DONE', last_error = NULL WHERE id = ?", jobId);
                log.info("Demo lịch sử lương: đã xử lý xong các kỳ 08–10/{}", YEAR);
            } catch (RuntimeException ex) {
                log.error("Demo lịch sử lương thất bại", ex);
                String msg = ex.getMessage() == null ? ex.getClass().getName() : ex.getMessage();
                jdbc.update("UPDATE background_job SET status = 'FAILED', last_error = ? WHERE id = ?",
                        msg.length() > 1900 ? msg.substring(0, 1900) : msg, jobId);
            } finally {
                SecurityContextHolder.clearContext();
            }
        }
    }

    private void process(int month) {
        Long id = jdbc.query("SELECT id FROM payroll_period WHERE period_year = ? AND period_month = ?",
                rs -> rs.next() ? rs.getLong(1) : null, YEAR, month);
        if (id == null) {
            return;
        }
        long hrLead = account("NV004");
        long hrExec = account("NV005");
        long director = account("NV001");
        long finance = account("NV003");
        String mm = String.format("%02d", month);

        // Máy trạng thái: tiếp tục từ bất kỳ bước nào nếu lần chạy trước dừng giữa chừng.
        for (int guard = 0; guard < 10; guard++) {
            Map<String, Object> p = jdbc.queryForMap("SELECT * FROM payroll_period WHERE id = ?", id);
            String status = (String) p.get("status");
            LocalDate end = ((Date) p.get("end_date")).toLocalDate();
            LocalDate payDate = ((Date) p.get("pay_date")).toLocalDate();
            if (("DRAFT".equals(status) || "CALCULATED".equals(status)) && p.get("conflict_decision_ref") == null) {
                as(hrLead, () -> payroll.conflictResolution(id, "NQ-HDTV-" + YEAR + "-" + mm,
                        "Hội đồng thành viên HNQ Media (mô phỏng)", end.plusDays(1),
                        "Lương Giám đốc do Hội đồng thành viên phê duyệt riêng; Giám đốc duyệt phần còn lại của bảng lương"));
            } else if ("DRAFT".equals(status) && !Boolean.TRUE.equals(p.get("timesheet_closed"))) {
                as(hrLead, () -> payroll.closeTimesheet(id));
            } else if ("DRAFT".equals(status)) {
                as(hrLead, () -> payroll.calculate(id));
            } else if ("CALCULATED".equals(status) && p.get("reconciled_at") == null) {
                as(hrExec, () -> payroll.reconcile(id, "Đối soát tổng Gross/BH/thuế/thực chi khớp; đã đối chiếu bảng công tháng " + month));
            } else if ("CALCULATED".equals(status)) {
                as(director, () -> payroll.approve(id));
            } else if ("APPROVED".equals(status)) {
                as(finance, () -> payroll.lock(id));
            } else if ("LOCKED".equals(status) && "UNPAID".equals(p.get("payment_status"))) {
                as(finance, () -> payroll.markPaid(id, "UNC-VCB-" + YEAR + mm + "-HNQ", payDate));
            } else {
                break;
            }
        }
        if (month == 8) {
            adjustmentsAfterAugust(id, hrLead, director);
        }
    }

    /** Sau khi kỳ 08 khóa: phát hiện sai sót -> điều chỉnh có lý do + minh chứng, Giám đốc duyệt, áp vào kỳ 09. */
    private void adjustmentsAfterAugust(long augustId, long hrLead, long director) {
        String status = jdbc.queryForObject("SELECT status FROM payroll_period WHERE id = ?", String.class, augustId);
        if (!"LOCKED".equals(status)) {
            return;
        }
        adjustment(augustId, "NV023", "GROSS", new BigDecimal("450000"), "TAXABLE",
                "Truy lĩnh 2 giờ OT ngày 08/08 chưa được ghi nhận vào kỳ 08", "Biên bản xác nhận OT ký ngày 03/09/2026",
                "ADJ-2026-08-NV023-OT", hrLead, director);
        adjustment(augustId, "NV031", "NET", new BigDecimal("-200000"), null,
                "Truy thu phụ cấp điện thoại đã chi trùng bằng tiền mặt trong tháng 08", "Biên bản đối chiếu phụ cấp ngày 05/09/2026",
                "ADJ-2026-08-NV031-PHONE", hrLead, director);
    }

    private void adjustment(long periodId, String code, String type, BigDecimal amount, String taxClass, String reason,
                            String evidence, String sourceRef, long creator, long approver) {
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM payroll_adjustment WHERE source_ref = ?", Integer.class, sourceRef);
        if (exists != null && exists > 0) {
            return;
        }
        Long employeeId = jdbc.queryForObject("SELECT id FROM employee WHERE code = ?", Long.class, code);
        as(creator, () -> payroll.createAdjustment(new AdjCmd(periodId, employeeId, type, amount, taxClass, reason, evidence, sourceRef)));
        Long adjId = jdbc.queryForObject("SELECT id FROM payroll_adjustment WHERE source_ref = ?", Long.class, sourceRef);
        as(approver, () -> payroll.decideAdjustment(adjId, true, "Đồng ý, áp dụng vào kỳ lương kế tiếp"));
    }

    private long account(String employeeCode) {
        return jdbc.queryForObject("""
                SELECT a.id FROM user_account a JOIN employee e ON e.id = a.employee_id WHERE e.code = ?
                """, Long.class, employeeCode);
    }

    private <T> T as(long accountId, Supplier<T> action) {
        Map<String, Object> a = jdbc.queryForMap("SELECT id, employee_id, email, role FROM user_account WHERE id = ?", accountId);
        AuthUser user = new AuthUser(((Number) a.get("id")).longValue(),
                a.get("employee_id") == null ? null : ((Number) a.get("employee_id")).longValue(),
                (String) a.get("email"), (String) a.get("role"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role()))));
        try {
            return action.get();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
