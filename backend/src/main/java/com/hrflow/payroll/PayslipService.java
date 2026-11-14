package com.hrflow.payroll;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import com.hrflow.common.audit.AuditService;
import com.hrflow.common.audit.NotificationService;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.util.Sql;
import com.hrflow.security.AuthUser;
import com.hrflow.security.PayrollAccess;
import com.hrflow.security.Principals;

/**
 * Phiếu lương PDF chỉ được tạo SAU khi kỳ được khóa (background_job GENERATE_PAYSLIPS, chống trùng bằng dedupe_key).
 * Lỗi PDF được ghi log và retry, không đảo trạng thái kỳ. Nhân viên chỉ xem phiếu đã phát hành của chính mình.
 */
@Service
public class PayslipService {

    private static final Logger log = LoggerFactory.getLogger(PayslipService.class);
    private static final int MAX_ATTEMPTS = 5;

    private static final String SELECT = """
            SELECT s.id, s.period_id, p.period_year AS year, p.period_month AS month, s.employee_id, e.code AS employee_code,
                   e.full_name AS employee_name, s.file_name, s.generated_at, s.published_at, l.gross, l.net_entitlement, l.payable
            FROM payslip s JOIN payroll_period p ON p.id = s.period_id JOIN employee e ON e.id = s.employee_id JOIN payroll_line l ON l.id = s.line_id
            """;

    private final JdbcTemplate jdbc;
    private final TransactionTemplate tx;
    private final PayrollAccess access;
    private final AuditService audit;
    private final NotificationService notifications;

    public PayslipService(JdbcTemplate jdbc, TransactionTemplate tx, PayrollAccess access, AuditService audit,
                          NotificationService notifications) {
        this.jdbc = jdbc;
        this.tx = tx;
        this.access = access;
        this.audit = audit;
        this.notifications = notifications;
    }

    // ---------------- tạo phiếu (job nền) ----------------

    @Scheduled(fixedDelay = 30000, initialDelay = 30000)
    public void runPendingJobs() {
        List<Map<String, Object>> jobs = jdbc.queryForList("""
                SELECT id, payload::text AS payload FROM background_job
                WHERE job_type = 'GENERATE_PAYSLIPS' AND status IN ('PENDING', 'FAILED') AND attempts < ? AND run_after <= now() ORDER BY id
                """, MAX_ATTEMPTS);
        for (Map<String, Object> job : jobs) {
            long jobId = ((Number) job.get("id")).longValue();
            String payload = (String) job.get("payload");
            long periodId = Long.parseLong(payload.replaceAll("[^0-9]", ""));
            long started = System.currentTimeMillis();
            try {
                jdbc.update("UPDATE background_job SET status = 'RUNNING', attempts = attempts + 1 WHERE id = ?", jobId);
                tx.executeWithoutResult(s -> generate(periodId));
                jdbc.update("UPDATE background_job SET status = 'DONE', last_error = NULL WHERE id = ?", jobId);
                jdbc.update("INSERT INTO job_run_log(job_name, run_key, started_at, finished_at, result, message) VALUES ('GENERATE_PAYSLIPS', ?, ?, now(), 'SUCCESS', NULL)",
                        "period:" + periodId, new Timestamp(started));
            } catch (RuntimeException ex) {
                log.error("Tạo phiếu lương kỳ {} thất bại, sẽ retry", periodId, ex);
                jdbc.update("UPDATE background_job SET status = 'FAILED', last_error = ?, run_after = now() + interval '1 minute' WHERE id = ?",
                        truncate(ex.getMessage()), jobId);
                jdbc.update("INSERT INTO job_run_log(job_name, run_key, started_at, finished_at, result, message) VALUES ('GENERATE_PAYSLIPS', ?, ?, now(), 'FAILED', ?)",
                        "period:" + periodId, new Timestamp(started), truncate(ex.getMessage()));
            }
        }
    }

    private static String truncate(String s) {
        return s == null ? null : s.length() > 1900 ? s.substring(0, 1900) : s;
    }

    /** Idempotent: uq_payslip(period, employee) bỏ qua phiếu đã có. */
    void generate(long periodId) {
        Map<String, Object> p = jdbc.queryForMap("SELECT period_year, period_month, status FROM payroll_period WHERE id = ?", periodId);
        if (!"LOCKED".equals(p.get("status"))) {
            throw new IllegalStateException("Kỳ chưa khóa, không tạo phiếu lương");
        }
        Instant now = Instant.now();
        List<Map<String, Object>> lines = jdbc.queryForList("SELECT * FROM payroll_line WHERE period_id = ?", periodId);
        for (Map<String, Object> l : lines) {
            long lineId = ((Number) l.get("id")).longValue();
            byte[] pdf = SimplePdf.render(pdfLines(p, l, lineId));
            String file = "phieu-luong-" + p.get("period_year") + "-" + String.format("%02d", ((Number) p.get("period_month")).intValue()) + "-" + l.get("employee_code") + ".pdf";
            int n = jdbc.update("""
                    INSERT INTO payslip(period_id, employee_id, line_id, file_name, pdf, generated_at, published_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?) ON CONFLICT (period_id, employee_id) DO NOTHING
                    """, periodId, l.get("employee_id"), lineId, file, pdf, Timestamp.from(now), Timestamp.from(now));
            if (n > 0) {
                notifications.toEmployee(((Number) l.get("employee_id")).longValue(), "PAYSLIP_PUBLISHED", "Phiếu lương mới",
                        "Phiếu lương tháng " + p.get("period_month") + "/" + p.get("period_year") + " đã sẵn sàng", "/payslips/me",
                        "payslip:" + periodId + ":" + l.get("employee_id"));
            }
        }
        jdbc.update("UPDATE payroll_period SET published_at = COALESCE(published_at, ?) WHERE id = ?", Timestamp.from(now), periodId);
    }

    private static final DecimalFormat MONEY = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));

    private static String m(Object v) {
        return MONEY.format((BigDecimal) v);
    }

    private static String pad(String label, String value) {
        return String.format("%-44s %18s", label, value);
    }

    private List<String> pdfLines(Map<String, Object> p, Map<String, Object> l, long lineId) {
        List<String> out = new ArrayList<>();
        out.add("CONG TY TNHH TRUYEN THONG HNQ (HNQ MEDIA)");
        out.add("PHIEU LUONG THANG " + p.get("period_month") + "/" + p.get("period_year"));
        out.add("");
        out.add("Ma NV: " + l.get("employee_code") + "    Ho ten: " + l.get("employee_name"));
        out.add("Phong ban: " + l.get("department_name") + "    Chuc danh: " + l.get("job_title_name"));
        out.add("Gio chuan: " + l.get("h_std") + "    Gio huong luong: " + l.get("h_paid"));
        out.add("------------------------------------------------------------------");
        out.add("THU NHAP (VND)");
        for (Map<String, Object> i : jdbc.queryForList("SELECT category, name, amount, source_ref FROM payroll_line_item WHERE line_id = ? ORDER BY seq", lineId)) {
            String cat = (String) i.get("category");
            if (List.of("INCOME", "ALLOWANCE", "OVERTIME").contains(cat) || ("ADJUSTMENT".equals(cat) && ((String) i.get("name")).contains("Gross"))) {
                out.add(pad("  " + i.get("name"), m(i.get("amount"))));
            }
        }
        out.add(pad("TONG THU NHAP (GROSS)", m(l.get("gross"))));
        out.add("");
        out.add("KHAU TRU (VND)");
        out.add(pad("  BHXH", m(l.get("bhxh"))));
        out.add(pad("  BHYT", m(l.get("bhyt"))));
        out.add(pad("  BHTN", m(l.get("bhtn"))));
        out.add(pad("  Thue TNCN", m(l.get("pit"))));
        out.add(pad("  Khau tru khac co can cu", m(l.get("legal_deductions"))));
        out.add(pad("  Dieu chinh Net", m(l.get("net_adjustment"))));
        out.add("------------------------------------------------------------------");
        out.add(pad("THUC NHAN SAU NGHIA VU (Net)", m(l.get("net_entitlement"))));
        out.add(pad("  Thu hoi tam ung", m(l.get("advance_recovery"))));
        out.add(pad("  Hoan chi", m(l.get("reimbursements"))));
        out.add(pad("THUC CHI (Payable)", m(l.get("payable"))));
        out.add("");
        out.add("Phieu duoc tao sau khi ky luong da khoa; khong sua sau khi phat hanh.");
        return out;
    }

    // ---------------- truy vấn ----------------

    public List<Map<String, Object>> mine() {
        return Sql.list(jdbc, SELECT + " WHERE s.employee_id = ? ORDER BY p.period_year DESC, p.period_month DESC", Principals.employeeId());
    }

    public List<Map<String, Object>> ofPeriod(long periodId) {
        access.require("VIEW", periodId);
        return Sql.list(jdbc, SELECT + " WHERE s.period_id = ? ORDER BY e.code", periodId);
    }

    private Map<String, Object> authorize(long id) {
        Map<String, Object> s = Sql.one(jdbc, "Không tìm thấy phiếu lương", SELECT + " WHERE s.id = ?", id);
        AuthUser u = Principals.current();
        boolean owner = u.employeeId() != null && u.employeeId().equals(((Number) s.get("employeeId")).longValue());
        if (!owner && !access.has("VIEW", ((Number) s.get("periodId")).longValue())) {
            audit.denied("VIEW_PAYSLIP", "payslip", id, "Truy cập phiếu lương của người khác không có grant VIEW");
            throw new ResourceNotFoundException("Không tìm thấy phiếu lương");
        }
        return s;
    }

    public Map<String, Object> get(long id) {
        return authorize(id);
    }

    public Map<String, Object> pdf(long id, boolean download) {
        authorize(id);
        Map<String, Object> row = jdbc.queryForMap("SELECT pdf, file_name FROM payslip WHERE id = ?", id);
        audit.success(download ? "DOWNLOAD_PAYSLIP" : "VIEW_PAYSLIP", "payslip", id, null, null, null);
        return row;
    }
}
