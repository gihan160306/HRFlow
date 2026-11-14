package com.hrflow.common;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
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
    public List<Map<String, Object>> headcount() {
        Principals.requireRole("HR", "ADMIN");
        return Sql.list(jdbc, """
                SELECT d.name AS department, e.employee_type, COUNT(*) AS headcount FROM employee e JOIN department d ON d.id = e.department_id
                WHERE e.status <> 'TERMINATED' GROUP BY d.name, e.employee_type ORDER BY d.name, e.employee_type
                """);
    }

    @GetMapping("/api/v1/reports/ot-hours")
    public List<Map<String, Object>> otHours(@RequestParam(required = false) Integer year) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        int y = year == null ? LocalDate.now().getYear() : year;
        String scope = u.is("HR") ? "" : " AND e.manager_id = " + u.employeeId();
        return Sql.list(jdbc, """
                SELECT EXTRACT(MONTH FROM s.work_date)::int AS month,
                       ROUND(SUM(CASE WHEN r.actual_status = 'CONFIRMED' THEN LEAST(COALESCE(r.actual_minutes, 0), COALESCE(r.approved_minutes, 0)) + r.extra_minutes_confirmed ELSE 0 END) / 60.0, 2) AS hours
                FROM ot_registration r JOIN ot_schedule s ON s.id = r.schedule_id JOIN employee e ON e.id = r.employee_id
                WHERE r.status = 'APPROVED' AND EXTRACT(YEAR FROM s.work_date) = ?""" + scope + " GROUP BY 1 ORDER BY 1", y);
    }
}
