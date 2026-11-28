package com.hrflow.common;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.response.PageResponse;
import com.hrflow.common.util.Sql;
import com.hrflow.security.AuthUser;
import com.hrflow.security.PayrollAccess;
import com.hrflow.security.Principals;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/** Thông báo, audit log, dashboard (không lộ lương), báo cáo vận hành, phân loại thuế loại phụ cấp. */
@RestController
public class MiscController {

    public record PreferenceRequest(Boolean emailEnabled) {
    }

    public record AllowanceClassifyRequest(@NotBlank String taxClass, @NotBlank String decisionRef, Boolean insuranceIncluded,
                                           Boolean includedInOtBase) {
    }

    private static final Set<String> PAYROLL_ENTITIES = Set.of("payroll_period", "payroll_line", "payroll_adjustment", "payslip",
            "payroll_grant", "salary_record", "payroll_parameter");
    private static final Set<String> EMP_STATUSES = Set.of("ACTIVE", "ONBOARDING", "ON_LEAVE", "TERMINATED");
    private static final Set<String> PERIOD_STATUSES = Set.of("DRAFT", "CALCULATED", "APPROVED", "LOCKED");
    private static final byte[] UTF8_BOM = new byte[] {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

    private static final Map<String, String> EMP_TYPE_LABELS = Map.of(
            "FULL_TIME", "Toàn thời gian",
            "PART_TIME", "Bán thời gian",
            "PROBATION", "Thử việc",
            "INTERN", "Thực tập sinh"
    );
    private static final Map<String, String> EMP_STATUS_LABELS = Map.of(
            "ACTIVE", "Đang làm việc",
            "ONBOARDING", "Đang tiếp nhận",
            "ON_LEAVE", "Đang nghỉ",
            "TERMINATED", "Đã nghỉ việc"
    );
    private static final Map<String, String> PERIOD_STATUS_LABELS = Map.of(
            "CALCULATED", "Tạm tính (Đã tính)",
            "APPROVED", "Đã duyệt",
            "LOCKED", "Đã khóa"
    );
    private static final Map<String, String> PAYMENT_STATUS_LABELS = Map.of(
            "PAID", "Đã chi trả",
            "UNPAID", "Chưa chi trả"
    );

    private final JdbcTemplate jdbc;
    private final PayrollAccess access;
    private final AuditService audit;

    public MiscController(JdbcTemplate jdbc, PayrollAccess access, AuditService audit) {
        this.jdbc = jdbc;
        this.access = access;
        this.audit = audit;
    }

    // ---------------- thông báo ----------------

    @GetMapping("/api/v1/notifications")
    public PageResponse<Map<String, Object>> notifications(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        long me = Principals.current().userId();
        int sz = Math.min(Math.max(size, 1), 100);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_account_id = ?", Long.class, me);
        List<Map<String, Object>> rows = Sql.list(jdbc, """
                SELECT id, notification_type AS type, title, message, link, read_at, created_at FROM notification
                WHERE recipient_account_id = ? ORDER BY id DESC LIMIT ? OFFSET ?
                """, me, sz, (long) Math.max(page, 0) * sz);
        return PageResponse.of(rows, Math.max(page, 0), sz, total == null ? 0 : total);
    }

    @GetMapping("/api/v1/notifications/unread-count")
    public Map<String, Object> unread() {
        return Map.of("count", jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_account_id = ? AND read_at IS NULL",
                Long.class, Principals.current().userId()));
    }

    @PostMapping("/api/v1/notifications/{id}/read")
    public ResponseEntity<Void> read(@PathVariable long id) {
        jdbc.update("UPDATE notification SET read_at = COALESCE(read_at, now()) WHERE id = ? AND recipient_account_id = ?", id, Principals.current().userId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/v1/notifications/read-all")
    public ResponseEntity<Void> readAll() {
        jdbc.update("UPDATE notification SET read_at = now() WHERE recipient_account_id = ? AND read_at IS NULL", Principals.current().userId());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/notifications/preferences")
    public Map<String, Object> preferences() {
        List<Boolean> v = jdbc.queryForList("SELECT email_enabled FROM notification_preference WHERE account_id = ?", Boolean.class, Principals.current().userId());
        return Map.of("emailEnabled", !v.isEmpty() && v.get(0));
    }

    @PutMapping("/api/v1/notifications/preferences")
    public Map<String, Object> savePreferences(@RequestBody PreferenceRequest r) {
        boolean on = r.emailEnabled() != null && r.emailEnabled();
        jdbc.update("""
                INSERT INTO notification_preference(account_id, email_enabled) VALUES (?, ?)
                ON CONFLICT (account_id) DO UPDATE SET email_enabled = EXCLUDED.email_enabled
                """, Principals.current().userId(), on);
        return Map.of("emailEnabled", on);
    }

    // ---------------- audit log ----------------

    @GetMapping("/api/v1/audit-logs")
    public PageResponse<Map<String, Object>> auditLogs(@RequestParam(required = false) String action, @RequestParam(required = false) String entityType,
                                                       @RequestParam(required = false) String result, @RequestParam(required = false) LocalDate from,
                                                       @RequestParam(required = false) LocalDate to, @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        AuthUser u = Principals.requireRole("ADMIN", "HR");
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (action != null && !action.isBlank()) {
            where.append(" AND action = ?");
            args.add(action);
        }
        if (entityType != null && !entityType.isBlank()) {
            where.append(" AND entity_type = ?");
            args.add(entityType);
        }
        if (result != null && !result.isBlank()) {
            where.append(" AND result = ?");
            args.add(result);
        }
        if (from != null) {
            where.append(" AND created_at >= ?::date");
            args.add(from.toString());
        }
        if (to != null) {
            where.append(" AND created_at < (?::date + 1)");
            args.add(to.toString());
        }
        int sz = Math.min(Math.max(size, 1), 200);
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM audit_log" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(sz);
        pageArgs.add((long) Math.max(page, 0) * sz);
        List<Map<String, Object>> rows = Sql.list(jdbc, """
                SELECT id, actor_name, action, entity_type, entity_id, result, reason, before_json, after_json, ip_address, created_at
                FROM audit_log""" + where + " ORDER BY id DESC LIMIT ? OFFSET ?", pageArgs.toArray());
        if (u.is("ADMIN")) {
            // Admin không xem giá trị trước/sau của dữ liệu lương nhạy cảm.
            for (Map<String, Object> r : rows) {
                if (PAYROLL_ENTITIES.contains(r.get("entityType"))) {
                    r.put("beforeJson", null);
                    r.put("afterJson", null);
                }
            }
        }
        return PageResponse.of(rows, Math.max(page, 0), sz, total == null ? 0 : total);
    }

    @GetMapping("/api/v1/job-runs")
    public List<Map<String, Object>> jobRuns() {
        Principals.requireRole("ADMIN", "HR");
        return Sql.list(jdbc, "SELECT id, job_name, run_key, started_at, finished_at, result, message FROM job_run_log ORDER BY id DESC LIMIT 100");
    }

    // ---------------- phân loại thuế loại phụ cấp (OPEN-04) ----------------

    @PatchMapping("/api/v1/allowance-types/{id}")
    @Transactional
    public Map<String, Object> classifyAllowanceType(@PathVariable long id, @Valid @RequestBody AllowanceClassifyRequest r) {
        Principals.requireRole("HR");
        if (!Set.of("TAXABLE", "NON_TAXABLE", "EXEMPT", "NEEDS_REVIEW").contains(r.taxClass())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Phân loại thuế không hợp lệ");
        }
        Map<String, Object> before = Sql.one(jdbc, "Không tìm thấy loại phụ cấp", "SELECT code, tax_class, insurance_included, included_in_ot_base FROM allowance_type WHERE id = ?", id);
        jdbc.update("""
                UPDATE allowance_type SET tax_class = ?, insurance_included = COALESCE(?, insurance_included),
                       included_in_ot_base = COALESCE(?, included_in_ot_base) WHERE id = ?
                """, r.taxClass(), r.insuranceIncluded(), r.includedInOtBase(), id);
        audit.success("CLASSIFY_ALLOWANCE_TYPE", "allowance_type", id, r.decisionRef(), before, Map.of("taxClass", r.taxClass()));
        return Sql.one(jdbc, "Không tìm thấy loại phụ cấp", "SELECT id, code, name, tax_class, insurance_included, included_in_ot_base, description FROM allowance_type WHERE id = ?", id);
    }

    // ---------------- dashboard / báo cáo (không lộ lương qua tổng hợp) ----------------

    @GetMapping("/api/v1/dashboard/summary")
    public Map<String, Object> dashboard() {
        AuthUser u = Principals.current();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", u.role());
        if (u.is("HR", "ADMIN")) {
            out.put("activeEmployees", jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE status = 'ACTIVE'", Long.class));
            out.put("onboarding", jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE status = 'ONBOARDING'", Long.class));
            out.put("expiringContracts30d", jdbc.queryForObject("SELECT COUNT(*) FROM contract WHERE status = 'ACTIVE' AND end_date IS NOT NULL AND end_date <= current_date + 30", Long.class));
            out.put("pendingAccountRequests", jdbc.queryForObject("SELECT COUNT(*) FROM account_request WHERE status = 'PENDING'", Long.class));
        }
        if (u.is("HR")) {
            out.put("attendanceNeedsReviewThisMonth", jdbc.queryForObject("""
                    SELECT COUNT(*) FROM attendance_record WHERE status = 'NEEDS_REVIEW' AND date_trunc('month', work_date) = date_trunc('month', current_date)
                    """, Long.class));
        }
        if (u.employeeId() != null) {
            long me = u.employeeId();
            out.put("teamSize", jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE manager_id = ? AND status <> 'TERMINATED'", Long.class, me));
            out.put("pendingLeaveForMe", jdbc.queryForObject("SELECT COUNT(*) FROM leave_request WHERE status = 'PENDING' AND employee_id <> ? AND primary_approver_id = ?", Long.class, me, me));
            out.put("pendingOtForMe", jdbc.queryForObject("SELECT COUNT(*) FROM ot_registration WHERE status = 'PENDING' AND employee_id <> ? AND primary_approver_id = ?", Long.class, me, me));
            out.put("myPendingLeave", jdbc.queryForObject("SELECT COUNT(*) FROM leave_request WHERE status = 'PENDING' AND employee_id = ?", Long.class, me));
        }
        if (access.hasAny(null, "VIEW", "PREPARE", "APPROVE", "LOCK")) {
            out.put("openPayrollPeriods", jdbc.queryForObject("SELECT COUNT(*) FROM payroll_period WHERE status <> 'LOCKED'", Long.class));
        }
        out.put("unreadNotifications", jdbc.queryForObject("SELECT COUNT(*) FROM notification WHERE recipient_account_id = ? AND read_at IS NULL", Long.class, u.userId()));
        return out;
    }

    @GetMapping("/api/v1/reports/headcount")
    public List<Map<String, Object>> headcount(@RequestParam(required = false) String status) {
        AuthUser u = Principals.requireRole("HR", "MANAGER", "ADMIN");
        return headcountRows(u, normalizeEmployeeStatus(status));
    }

    @GetMapping("/api/v1/reports/headcount/export")
    public ResponseEntity<byte[]> headcountExport(@RequestParam(required = false) String status) {
        AuthUser u = Principals.requireRole("HR", "MANAGER", "ADMIN");
        String normStatus = normalizeEmployeeStatus(status);
        List<Map<String, Object>> rows = headcountRows(u, normStatus);
        StringBuilder sb = new StringBuilder("Phòng ban,Loại nhân sự,Trạng thái,Số lượng\n");
        for (Map<String, Object> r : rows) {
            String empType = String.valueOf(r.get("employeeType"));
            String empStatus = String.valueOf(r.get("status"));
            sb.append(csvText(r.get("department"))).append(',')
                    .append(csvText(EMP_TYPE_LABELS.getOrDefault(empType, empType))).append(',')
                    .append(csvText(EMP_STATUS_LABELS.getOrDefault(empStatus, empStatus))).append(',')
                    .append(csvNumber(r.get("headcount"))).append('\n');
        }
        byte[] bytes = utf8BomCsv(sb.toString());
        String scope = isCompanyHeadcountScope(u) ? "COMPANY" : "TEAM";
        audit.success("EXPORT_REPORT_HEADCOUNT", "report", null, null, null,
                Map.of("scope", scope, "status", normStatus == null ? "DEFAULT_ACTIVE" : normStatus, "rows", rows.size()));
        return csvResponse("bao-cao-nhan-su.csv", bytes);
    }

    @GetMapping("/api/v1/reports/ot-hours")
    public List<Map<String, Object>> otHours(@RequestParam(required = false) Integer year) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        int y = validateReportYear(year, LocalDate.now().getYear());
        return otHoursRows(u, y);
    }

    @GetMapping("/api/v1/reports/ot-hours/export")
    public ResponseEntity<byte[]> otHoursExport(@RequestParam(required = false) Integer year) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        int y = validateReportYear(year, LocalDate.now().getYear());
        List<Map<String, Object>> rows = otHoursRows(u, y);
        StringBuilder sb = new StringBuilder("Năm,Tháng,Giờ OT đã xác nhận\n");
        for (Map<String, Object> r : rows) {
            sb.append(y).append(',')
                    .append(csvNumber(r.get("month"))).append(',')
                    .append(csvNumber(r.get("hours"))).append('\n');
        }
        byte[] bytes = utf8BomCsv(sb.toString());
        String scope = u.is("HR") ? "COMPANY" : "TEAM";
        audit.success("EXPORT_REPORT_OT", "report", null, null, null,
                Map.of("scope", scope, "year", y, "rows", rows.size()));
        return csvResponse("bao-cao-gio-ot-" + y + ".csv", bytes);
    }

    @GetMapping("/api/v1/reports/payroll-fund")
    public List<Map<String, Object>> payrollFund(@RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) String status,
                                                 @RequestParam(required = false) Long periodId) {
        AuthUser u = Principals.current();
        Integer validYear = year == null ? null : validateReportYear(year, year);
        String validStatus = normalizePeriodStatus(status);
        ViewGrantScope grantScope = resolveViewGrantScope(u, periodId);
        return payrollFundRows(grantScope, validYear, validStatus, periodId);
    }

    @GetMapping("/api/v1/reports/payroll-fund/export")
    public ResponseEntity<byte[]> payrollFundExport(@RequestParam(required = false) Integer year,
                                                    @RequestParam(required = false) String status,
                                                    @RequestParam(required = false) Long periodId) {
        AuthUser u = Principals.current();
        Integer validYear = year == null ? null : validateReportYear(year, year);
        String validStatus = normalizePeriodStatus(status);
        ViewGrantScope grantScope = resolveViewGrantScope(u, periodId);
        List<Map<String, Object>> rows = payrollFundRows(grantScope, validYear, validStatus, periodId);
        StringBuilder sb = new StringBuilder("Kỳ lương,Trạng thái kỳ,Chi trả,Số nhân sự,Tổng Gross (VND),Tổng BH NLĐ (VND),Tổng thuế TNCN (VND),Tổng Net sau nghĩa vụ (VND),Tổng Thực chi Payable (VND)\n");
        for (Map<String, Object> r : rows) {
            String periodLabel = String.format("%02d/%d", ((Number) r.get("month")).intValue(), ((Number) r.get("year")).intValue());
            String st = String.valueOf(r.get("status"));
            String pst = String.valueOf(r.get("paymentStatus"));
            sb.append(csvText(periodLabel)).append(',')
                    .append(csvText(PERIOD_STATUS_LABELS.getOrDefault(st, st))).append(',')
                    .append(csvText(PAYMENT_STATUS_LABELS.getOrDefault(pst, pst))).append(',')
                    .append(csvNumber(r.get("employeeCount"))).append(',')
                    .append(csvNumber(r.get("totalGross"))).append(',')
                    .append(csvNumber(r.get("totalInsurance"))).append(',')
                    .append(csvNumber(r.get("totalPit"))).append(',')
                    .append(csvNumber(r.get("totalNet"))).append(',')
                    .append(csvNumber(r.get("totalPayable"))).append('\n');
        }
        byte[] bytes = utf8BomCsv(sb.toString());
        Map<String, Object> auditMeta = new LinkedHashMap<>();
        auditMeta.put("scope", grantScope.global() ? "ALL_PERIODS" : "SCOPED_PERIODS");
        auditMeta.put("year", validYear);
        auditMeta.put("status", validStatus);
        auditMeta.put("rows", rows.size());
        audit.success("EXPORT_REPORT_PAYROLL_FUND", "payroll_period", periodId, null, null, auditMeta);
        return csvResponse("bao-cao-quy-luong.csv", bytes);
    }

    private boolean isCompanyHeadcountScope(AuthUser u) {
        return u.is("HR", "ADMIN");
    }

    private String normalizeEmployeeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String s = status.trim().toUpperCase();
        if (!"ALL".equals(s) && !EMP_STATUSES.contains(s)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Trạng thái nhân sự không hợp lệ");
        }
        return s;
    }

    private int validateReportYear(Integer year, int defaultYear) {
        int y = year == null ? defaultYear : year;
        if (y < 2000 || y > 2100) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Năm báo cáo phải nằm trong khoảng 2000–2100");
        }
        return y;
    }

    private String normalizePeriodStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String s = status.trim().toUpperCase();
        if (!PERIOD_STATUSES.contains(s)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Trạng thái kỳ lương không hợp lệ");
        }
        return s;
    }

    private List<Map<String, Object>> headcountRows(AuthUser u, String status) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (status == null) {
            where.append(" AND e.status <> 'TERMINATED'");
        } else if (!"ALL".equals(status)) {
            where.append(" AND e.status = ?");
            args.add(status);
        }
        if (!isCompanyHeadcountScope(u)) {
            where.append(" AND e.manager_id = ?");
            args.add(Principals.employeeId());
        }
        return Sql.list(jdbc, """
                SELECT d.name AS department, e.employee_type, e.status, COUNT(*) AS headcount
                FROM employee e JOIN department d ON d.id = e.department_id""" + where + " "
                + "GROUP BY d.name, e.employee_type, e.status ORDER BY d.name, e.employee_type, e.status", args.toArray());
    }

    private List<Map<String, Object>> otHoursRows(AuthUser u, int year) {
        List<Object> args = new ArrayList<>();
        String scopeSql = "";
        Long managerId = null;
        if (!u.is("HR")) {
            scopeSql = " AND e.manager_id = ?";
            managerId = Principals.employeeId();
        }
        args.add(year);
        if (managerId != null) {
            args.add(managerId);
        }
        args.add(year);
        if (managerId != null) {
            args.add(managerId);
        }
        // Kết hợp OT theo lịch đã xác nhận và OT ngoài lịch đã xác nhận (đã tách đoạn qua nửa đêm theo ngày trong BUG-14).
        return Sql.list(jdbc, """
                WITH ot_entries AS (
                    SELECT EXTRACT(MONTH FROM s.work_date)::int AS month,
                           (LEAST(COALESCE(r.actual_minutes, 0), COALESCE(r.approved_minutes, 0)) + r.extra_minutes_confirmed) AS mins
                    FROM ot_registration r
                    JOIN ot_schedule s ON s.id = r.schedule_id
                    JOIN employee e ON e.id = r.employee_id
                    WHERE r.status = 'APPROVED' AND r.actual_status = 'CONFIRMED'
                      AND EXTRACT(YEAR FROM s.work_date) = ?""" + scopeSql + "\n" + """
                    UNION ALL
                    SELECT EXTRACT(MONTH FROM o.work_date)::int AS month,
                           o.minutes AS mins
                    FROM ot_unscheduled o
                    JOIN employee e ON e.id = o.employee_id
                    WHERE o.status = 'CONFIRMED'
                      AND EXTRACT(YEAR FROM o.work_date) = ?""" + scopeSql + "\n" + """
                )
                SELECT month, ROUND(SUM(mins) / 60.0, 2) AS hours
                FROM ot_entries
                GROUP BY month
                ORDER BY month
                """, args.toArray());
    }

    private record ViewGrantScope(boolean global, List<Long> periodIds) {
    }

    private ViewGrantScope resolveViewGrantScope(AuthUser u, Long requestedPeriodId) {
        if (requestedPeriodId != null) {
            access.require("VIEW", requestedPeriodId);
        }
        if (access.has("VIEW", null)) {
            return new ViewGrantScope(true, List.of());
        }
        List<Long> periodIds = jdbc.queryForList("""
                SELECT DISTINCT g.period_id FROM payroll_grant g JOIN user_account a ON a.id = g.account_id
                WHERE g.account_id = ? AND g.permission = 'VIEW' AND a.status = 'ACTIVE' AND a.role <> 'ADMIN'
                  AND g.revoked_at IS NULL AND g.valid_from <= now()
                  AND (g.valid_to IS NULL OR g.valid_to > now()) AND g.period_id IS NOT NULL
                ORDER BY g.period_id
                """, Long.class, u.userId());
        if (periodIds.isEmpty()) {
            audit.denied("PAYROLL_ACCESS", "payroll_grant", requestedPeriodId, "Thiếu grant VIEW");
            throw new AccessDeniedException("Thiếu quyền payroll VIEW");
        }
        return new ViewGrantScope(false, periodIds);
    }

    private List<Map<String, Object>> payrollFundRows(ViewGrantScope scope, Integer year, String status, Long periodId) {
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder("""
                 WHERE p.status IN ('CALCULATED', 'APPROVED', 'LOCKED')
                   AND p.calc_version > 0
                """);
        if (!scope.global()) {
            String placeholders = scope.periodIds().stream().map(x -> "?").collect(Collectors.joining(", "));
            where.append(" AND p.id IN (").append(placeholders).append(")");
            args.addAll(scope.periodIds());
        }
        if (periodId != null) {
            where.append(" AND p.id = ?");
            args.add(periodId);
        }
        if (year != null) {
            where.append(" AND p.period_year = ?");
            args.add(year);
        }
        if (status != null) {
            where.append(" AND p.status = ?");
            args.add(status);
        }
        return Sql.list(jdbc, """
                SELECT p.id AS period_id,
                       p.period_year AS year,
                       p.period_month AS month,
                       p.pay_date,
                       p.status,
                       p.payment_status,
                       p.calc_version,
                       COUNT(l.id) AS employee_count,
                       SUM(l.gross) AS total_gross,
                       SUM(l.insurance_total) AS total_insurance,
                       SUM(l.pit) AS total_pit,
                       SUM(l.net_entitlement) AS total_net,
                       SUM(l.payable) AS total_payable,
                       SUM(l.advance_recovery) AS total_advance_recovery,
                       SUM(l.reimbursements) AS total_reimbursements
                FROM payroll_period p
                JOIN payroll_line l ON l.period_id = p.id AND l.calc_version = p.calc_version""" + "\n" + where + "\n" + """
                GROUP BY p.id, p.period_year, p.period_month, p.pay_date, p.status, p.payment_status, p.calc_version
                HAVING COUNT(l.id) > 0
                ORDER BY p.period_year DESC, p.period_month DESC
                """, args.toArray());
    }

    static String csvText(Object value) {
        if (value == null) {
            return "";
        }
        String s = String.valueOf(value);
        if (!s.isEmpty()) {
            char first = s.charAt(0);
            String trimmed = s.stripLeading();
            char lead = trimmed.isEmpty() ? first : trimmed.charAt(0);
            if (first == '=' || first == '+' || first == '-' || first == '@' || first == '\t' || first == '\r'
                    || lead == '=' || lead == '+' || lead == '-' || lead == '@') {
                s = "'" + s;
            }
        }
        return quoteCsvIfNeeded(s);
    }

    static String csvNumber(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof BigDecimal bd) {
            return bd.stripTrailingZeros().scale() <= 0
                    ? bd.setScale(0, java.math.RoundingMode.UNNECESSARY).toPlainString()
                    : bd.stripTrailingZeros().toPlainString();
        }
        return String.valueOf(value);
    }

    private static String quoteCsvIfNeeded(String s) {
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    static byte[] utf8BomCsv(String csv) {
        byte[] content = csv.getBytes(StandardCharsets.UTF_8);
        byte[] out = new byte[UTF8_BOM.length + content.length];
        System.arraycopy(UTF8_BOM, 0, out, 0, UTF8_BOM.length);
        System.arraycopy(content, 0, out, UTF8_BOM.length, content.length);
        return out;
    }

    private static ResponseEntity<byte[]> csvResponse(String filename, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }
}
