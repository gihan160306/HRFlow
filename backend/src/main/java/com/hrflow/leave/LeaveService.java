package com.hrflow.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.attendance.WorkCalendar;
import com.hrflow.common.Approvals;
import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.ApprovalLogService;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.audit.NotificationService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.util.Sql;
import com.hrflow.common.util.Vn;
import com.hrflow.hr.EmployeeService;
import com.hrflow.leave.LeaveCalc.DayLeave;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/**
 * Nghỉ phép (nhóm D). Duyệt một cấp theo approval_route có người dự phòng; chặn tự duyệt; nếu không có người
 * duyệt hợp lệ thì đơn giữ PENDING (không còn quy tắc "HR duyệt khi không có Manager").
 * Nửa ngày và số ngày nghỉ tính theo lịch cá nhân; kiểm tra số dư và trùng lịch.
 */
@Service
public class LeaveService {

    private static final String SELECT = """
            SELECT r.id, r.employee_id, e.code AS employee_code, e.full_name AS employee_name, r.leave_type_id,
                   t.code AS leave_type_code, t.name AS leave_type_name, t.paid, r.kind, r.parent_request_id, r.unit,
                   r.start_date, r.end_date, r.start_time, r.end_time, r.duration_minutes, r.duration_days, r.reason,
                   r.evidence, r.status, r.primary_approver_id, pe.full_name AS primary_approver_name,
                   r.backup_approver_id, be.full_name AS backup_approver_name, r.submitted_at,
                   de.full_name AS decided_by_name, r.decided_at, r.decision_reason, r.used_backup, r.backup_reason
            FROM leave_request r JOIN employee e ON e.id = r.employee_id JOIN leave_type t ON t.id = r.leave_type_id
            LEFT JOIN employee pe ON pe.id = r.primary_approver_id LEFT JOIN employee be ON be.id = r.backup_approver_id
            LEFT JOIN employee de ON de.id = r.decided_by
            """;

    private final JdbcTemplate jdbc;
    private final WorkCalendar calendar;
    private final Approvals approvals;
    private final PeriodGuard guard;
    private final AuditService audit;
    private final ApprovalLogService approvalLog;
    private final NotificationService notifications;
    private final EmployeeService employees;

    public LeaveService(JdbcTemplate jdbc, WorkCalendar calendar, Approvals approvals, PeriodGuard guard,
                        AuditService audit, ApprovalLogService approvalLog, NotificationService notifications,
                        EmployeeService employees) {
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.approvals = approvals;
        this.guard = guard;
        this.audit = audit;
        this.approvalLog = approvalLog;
        this.notifications = notifications;
        this.employees = employees;
    }

    public record Body(long leaveTypeId, String unit, LocalDate startDate, LocalDate endDate, LocalTime startTime,
                       LocalTime endTime, String reason, String evidence) {
    }

    // ---------------- preview / tạo / sửa ----------------

    private List<DayLeave> compute(long employeeId, Body b) {
        if (b.endDate().isBefore(b.startDate())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày kết thúc phải từ ngày bắt đầu trở đi");
        }
        if (b.startDate().until(b.endDate()).getDays() > 120 || b.startDate().until(b.endDate()).toTotalMonths() > 3) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Khoảng nghỉ tối đa 3 tháng cho một đơn");
        }
        return LeaveCalc.compute(calendar.plans(employeeId, b.startDate(), b.endDate()), b.unit(), b.startDate(),
                b.endDate(), b.startTime(), b.endTime());
    }

    public Map<String, Object> preview(Body b) {
        List<DayLeave> days = compute(Principals.employeeId(), b);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("days", days.stream().map(d -> Map.of("date", d.date(), "minutes", d.minutes(), "dayFraction", d.dayFraction())).toList());
        out.put("totalMinutes", days.stream().mapToInt(DayLeave::minutes).sum());
        out.put("totalDays", days.stream().map(DayLeave::dayFraction).reduce(BigDecimal.ZERO, BigDecimal::add));
        return out;
    }

    @Transactional
    public Map<String, Object> create(Body b, boolean submit) {
        long emp = Principals.employeeId();
        requireWorking(emp);
        validateBody(b);
        List<DayLeave> days = compute(emp, b);
        Long id = jdbc.queryForObject("""
                INSERT INTO leave_request(employee_id, leave_type_id, kind, unit, start_date, end_date, start_time, end_time,
                       duration_minutes, duration_days, reason, evidence, status)
                VALUES (?, ?, 'NEW', ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT') RETURNING id
                """, Long.class, emp, b.leaveTypeId(), b.unit(), b.startDate(), b.endDate(), b.startTime(), b.endTime(),
                days.stream().mapToInt(DayLeave::minutes).sum(),
                days.stream().map(DayLeave::dayFraction).reduce(BigDecimal.ZERO, BigDecimal::add), b.reason(), b.evidence());
        replaceDays(id, days);
        approvalLog.log("LEAVE_REQUEST", id, "CREATE", null, "DRAFT", b.reason(), false);
        if (submit) {
            doSubmit(id);
        }
        return get(id);
    }

    @Transactional
    public Map<String, Object> update(long id, Body b) {
        long emp = Principals.employeeId();
        Map<String, Object> r = lockOwn(id, emp);
        if (!"DRAFT".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ sửa được đơn nháp");
        }
        validateBody(b);
        List<DayLeave> days = compute(emp, b);
        jdbc.update("""
                UPDATE leave_request SET leave_type_id = ?, unit = ?, start_date = ?, end_date = ?, start_time = ?, end_time = ?,
                       duration_minutes = ?, duration_days = ?, reason = ?, evidence = ?, version = version + 1 WHERE id = ?
                """, b.leaveTypeId(), b.unit(), b.startDate(), b.endDate(), b.startTime(), b.endTime(),
                days.stream().mapToInt(DayLeave::minutes).sum(),
                days.stream().map(DayLeave::dayFraction).reduce(BigDecimal.ZERO, BigDecimal::add), b.reason(), b.evidence(), id);
        replaceDays(id, days);
        return get(id);
    }

    private void validateBody(Body b) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM leave_type WHERE id = ?", Integer.class, b.leaveTypeId());
        if (n == null || n == 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Loại nghỉ không tồn tại");
        }
    }

    private void replaceDays(long id, List<DayLeave> days) {
        jdbc.update("DELETE FROM leave_request_day WHERE leave_request_id = ?", id);
        for (DayLeave d : days) {
            jdbc.update("INSERT INTO leave_request_day(leave_request_id, leave_date, minutes, day_fraction) VALUES (?, ?, ?, ?)",
                    id, d.date(), d.minutes(), d.dayFraction());
        }
    }

    // ---------------- gửi / hủy ----------------

    @Transactional
    public Map<String, Object> submit(long id) {
        long emp = Principals.employeeId();
        lockOwn(id, emp);
        doSubmit(id);
        return get(id);
    }

    private void doSubmit(long id) {
        Map<String, Object> r = Sql.one(jdbc, "Không tìm thấy đơn", SELECT + " WHERE r.id = ?", id);
        if (!"DRAFT".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ gửi được đơn nháp");
        }
        long emp = ((Number) r.get("employeeId")).longValue();
        LocalDate start = (LocalDate) r.get("startDate");
        LocalDate end = (LocalDate) r.get("endDate");
        guard.assertOpen(start, end);
        Map<String, Object> type = jdbc.queryForMap("SELECT deduct_balance, requires_evidence FROM leave_type WHERE id = ?", r.get("leaveTypeId"));
        if (Boolean.TRUE.equals(type.get("requires_evidence")) && (r.get("evidence") == null || ((String) r.get("evidence")).isBlank())) {
            throw BusinessException.unprocessable("EVIDENCE_REQUIRED", "Loại nghỉ này bắt buộc có minh chứng");
        }
        assertNoOverlap(id, emp, r);
        if (Boolean.TRUE.equals(type.get("deduct_balance"))) {
            assertBalance(id, emp);
        }
        Approvals.Route route = approvals.routeOf(emp, start);
        jdbc.update("""
                UPDATE leave_request SET status = 'PENDING', submitted_at = now(), primary_approver_id = ?, backup_approver_id = ?,
                       version = version + 1 WHERE id = ?
                """, route.primaryId(), route.backupId(), id);
        approvalLog.log("LEAVE_REQUEST", id, "SUBMIT", "DRAFT", "PENDING", null, false);
        String name = (String) r.get("employeeName");
        notifications.toEmployee(route.primaryId(), "LEAVE_PENDING", "Đơn nghỉ chờ duyệt",
                name + " gửi đơn nghỉ " + start + (start.equals(end) ? "" : " → " + end), "/leave-requests/inbox", "leave-submit:" + id);
        if (route.primaryId() == null) {
            audit.success("LEAVE_NO_APPROVER", "leave_request", id, "Không có người duyệt hợp lệ theo approval_route; đơn giữ PENDING", null, null);
        }
    }

    @Transactional
    public Map<String, Object> cancel(long id) {
        long emp = Principals.employeeId();
        Map<String, Object> r = lockOwn(id, emp);
        String status = (String) r.get("status");
        if (!"DRAFT".equals(status) && !"PENDING".equals(status)) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ hủy được đơn chưa duyệt; đơn đã duyệt phải gửi yêu cầu hủy");
        }
        jdbc.update("UPDATE leave_request SET status = 'CANCELLED', version = version + 1 WHERE id = ?", id);
        approvalLog.log("LEAVE_REQUEST", id, "CANCEL", status, "CANCELLED", null, false);
        return get(id);
    }

    @Transactional
    public Map<String, Object> requestCancel(long id, String reason) {
        long emp = Principals.employeeId();
        Map<String, Object> p = lockOwn(id, emp);
        if (!"APPROVED".equals(p.get("status")) || !"NEW".equals(p.get("kind"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ yêu cầu hủy được đơn đã duyệt");
        }
        Integer open = jdbc.queryForObject("SELECT COUNT(*) FROM leave_request WHERE parent_request_id = ? AND status = 'PENDING'", Integer.class, id);
        if (open != null && open > 0) {
            throw BusinessException.conflict("INVALID_STATE", "Đã có yêu cầu hủy đang chờ duyệt");
        }
        guard.assertOpen((LocalDate) p.get("startDate"), (LocalDate) p.get("endDate"));
        Approvals.Route route = approvals.routeOf(emp, (LocalDate) p.get("startDate"));
        Long cid = jdbc.queryForObject("""
                INSERT INTO leave_request(employee_id, leave_type_id, kind, parent_request_id, unit, start_date, end_date, start_time,
                       end_time, duration_minutes, duration_days, reason, status, primary_approver_id, backup_approver_id, submitted_at)
                VALUES (?, ?, 'CANCEL_REQUEST', ?, ?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?, ?, now()) RETURNING id
                """, Long.class, emp, p.get("leave_type_id"), id, p.get("unit"), p.get("start_date"), p.get("end_date"),
                p.get("start_time"), p.get("end_time"), p.get("duration_minutes"), p.get("duration_days"), reason,
                route.primaryId(), route.backupId());
        approvalLog.log("LEAVE_REQUEST", cid, "CANCEL_REQUEST", null, "PENDING", reason, false);
        notifications.toEmployee(route.primaryId(), "LEAVE_PENDING", "Yêu cầu hủy đơn nghỉ",
                "Có yêu cầu hủy đơn nghỉ đã duyệt chờ xử lý", "/leave-requests/inbox", "leave-cancel:" + cid);
        return get(cid);
    }

    // ---------------- duyệt / từ chối ----------------

    @Transactional
    public Map<String, Object> approve(long id, String reason, String backupReason) {
        return decide(id, true, reason, backupReason);
    }

    @Transactional
    public Map<String, Object> reject(long id, String reason, String backupReason) {
        if (reason == null || reason.isBlank()) {
            throw BusinessException.unprocessable("REASON_REQUIRED", "Từ chối đơn phải ghi lý do");
        }
        return decide(id, false, reason, backupReason);
    }

    private Map<String, Object> decide(long id, boolean approve, String reason, String backupReason) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM leave_request WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn nghỉ");
        }
        Map<String, Object> r = rows.get(0);
        long emp = ((Number) r.get("employee_id")).longValue();
        boolean usedBackup = approvals.authorize("LEAVE_REQUEST", id, emp, asLong(r.get("primary_approver_id")),
                asLong(r.get("backup_approver_id")), backupReason);
        if (!"PENDING".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Đơn không ở trạng thái chờ duyệt");
        }
        LocalDate start = ((java.sql.Date) r.get("start_date")).toLocalDate();
        LocalDate end = ((java.sql.Date) r.get("end_date")).toLocalDate();
        guard.assertOpen(start, end);
        AuthUser u = Principals.current();
        String to = approve ? "APPROVED" : "REJECTED";
        jdbc.update("""
                UPDATE leave_request SET status = ?, decided_by = ?, decided_at = now(), decision_reason = ?, used_backup = ?,
                       backup_reason = ?, version = version + 1 WHERE id = ?
                """, to, u.employeeId(), reason, usedBackup, usedBackup ? backupReason : null, id);
        String kind = (String) r.get("kind");
        if (approve) {
            if ("NEW".equals(kind)) {
                assertNoOverlap(id, emp, Sql.camel(r));
                if (deducts(((Number) r.get("leave_type_id")).longValue())) {
                    applyUsage(id, emp, true);
                }
            } else {
                long parent = ((Number) r.get("parent_request_id")).longValue();
                jdbc.update("UPDATE leave_request SET status = 'CANCELLED', version = version + 1 WHERE id = ? AND status = 'APPROVED'", parent);
                if (deducts(((Number) r.get("leave_type_id")).longValue())) {
                    applyUsage(parent, emp, false);
                }
                approvalLog.log("LEAVE_REQUEST", parent, "CANCELLED_BY_REQUEST", "APPROVED", "CANCELLED", reason, usedBackup);
            }
        }
        approvalLog.log("LEAVE_REQUEST", id, approve ? "APPROVE" : "REJECT", "PENDING", to, reason, usedBackup);
        notifications.toEmployee(emp, approve ? "LEAVE_APPROVED" : "LEAVE_REJECTED",
                approve ? "Đơn nghỉ được duyệt" : "Đơn nghỉ bị từ chối", "Đơn nghỉ " + start + " đã " + (approve ? "được duyệt" : "bị từ chối"),
                "/leave-requests/me", "leave-decision:" + id);
        return get(id);
    }

    // ---------------- số dư ----------------

    private boolean deducts(long leaveTypeId) {
        return Boolean.TRUE.equals(jdbc.queryForObject("SELECT deduct_balance FROM leave_type WHERE id = ?", Boolean.class, leaveTypeId));
    }

    /** Trừ/hoàn quỹ phép theo từng năm của các ngày nghỉ trong đơn. */
    private void applyUsage(long requestId, long employeeId, boolean use) {
        List<Map<String, Object>> perYear = jdbc.queryForList("""
                SELECT EXTRACT(YEAR FROM leave_date)::int AS y, SUM(day_fraction) AS days
                FROM leave_request_day WHERE leave_request_id = ? GROUP BY 1
                """, requestId);
        for (Map<String, Object> py : perYear) {
            int year = ((Number) py.get("y")).intValue();
            BigDecimal days = (BigDecimal) py.get("days");
            Long balanceId = balanceId(employeeId, year, true);
            BigDecimal delta = use ? days.negate() : days;
            jdbc.update("UPDATE leave_balance SET used_days = used_days + ?, version = version + 1 WHERE id = ?", use ? days : days.negate(), balanceId);
            jdbc.update("""
                    INSERT INTO leave_balance_txn(balance_id, txn_type, delta_days, leave_request_id, reason, created_by)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, balanceId, use ? "USED" : "REFUND", delta, requestId, use ? "Duyệt đơn nghỉ" : "Hủy đơn nghỉ đã duyệt", Principals.current().userId());
        }
    }

    private Long balanceId(long employeeId, int year, boolean create) {
        Long id = jdbc.query("SELECT id FROM leave_balance WHERE employee_id = ? AND balance_year = ?",
                rs -> rs.next() ? rs.getLong(1) : null, employeeId, year);
        if (id == null && create) {
            id = jdbc.queryForObject("INSERT INTO leave_balance(employee_id, balance_year) VALUES (?, ?) RETURNING id", Long.class, employeeId, year);
        }
        return id;
    }

    private void assertBalance(long requestId, long employeeId) {
        List<Map<String, Object>> perYear = jdbc.queryForList("""
                SELECT EXTRACT(YEAR FROM leave_date)::int AS y, SUM(day_fraction) AS days
                FROM leave_request_day WHERE leave_request_id = ? GROUP BY 1
                """, requestId);
        for (Map<String, Object> py : perYear) {
            int year = ((Number) py.get("y")).intValue();
            BigDecimal need = (BigDecimal) py.get("days");
            BigDecimal remaining = remaining(employeeId, year, requestId);
            if (remaining.compareTo(need) < 0) {
                throw BusinessException.unprocessable("INSUFFICIENT_BALANCE",
                        "Không đủ số dư phép năm " + year + ": còn " + remaining.stripTrailingZeros().toPlainString() + " ngày, cần "
                                + need.stripTrailingZeros().toPlainString() + " ngày");
            }
        }
    }

    /** Số dư còn lại = đầu kỳ + tích lũy + điều chỉnh − đã dùng − đang chờ duyệt (trừ chính đơn đang xét). */
    private BigDecimal remaining(long employeeId, int year, Long excludeRequest) {
        BigDecimal base = jdbc.query("""
                SELECT opening_days + accrued_days + adjusted_days - used_days FROM leave_balance WHERE employee_id = ? AND balance_year = ?
                """, rs -> rs.next() ? rs.getBigDecimal(1) : BigDecimal.ZERO, employeeId, year);
        BigDecimal pending = pendingDays(employeeId, year, excludeRequest);
        return base.subtract(pending);
    }

    private BigDecimal pendingDays(long employeeId, int year, Long excludeRequest) {
        BigDecimal v = jdbc.queryForObject("""
                SELECT COALESCE(SUM(d.day_fraction), 0) FROM leave_request_day d
                JOIN leave_request r ON r.id = d.leave_request_id JOIN leave_type t ON t.id = r.leave_type_id
                WHERE r.employee_id = ? AND r.kind = 'NEW' AND r.status = 'PENDING' AND t.deduct_balance
                  AND EXTRACT(YEAR FROM d.leave_date) = ? AND (?::bigint IS NULL OR r.id <> ?)
                """, BigDecimal.class, employeeId, year, excludeRequest, excludeRequest);
        return v == null ? BigDecimal.ZERO : v;
    }

    private void assertNoOverlap(long requestId, long employeeId, Map<String, Object> r) {
        String unit = (String) r.get("unit");
        List<Map<String, Object>> clashes = jdbc.queryForList("""
                SELECT DISTINCT o.id, o.unit, o.start_time, o.end_time FROM leave_request o
                JOIN leave_request_day od ON od.leave_request_id = o.id
                JOIN leave_request_day nd ON nd.leave_request_id = ? AND nd.leave_date = od.leave_date
                WHERE o.employee_id = ? AND o.id <> ? AND o.kind = 'NEW' AND o.status IN ('PENDING', 'APPROVED')
                """, requestId, employeeId, requestId);
        for (Map<String, Object> o : clashes) {
            String ou = (String) o.get("unit");
            boolean conflict = "FULL_DAY".equals(unit) || "FULL_DAY".equals(ou) || unit.equals(ou) && !"HOURS".equals(unit);
            if ("HOURS".equals(unit) && "HOURS".equals(ou)) {
                LocalTime s1 = (LocalTime) r.get("startTime");
                LocalTime e1 = (LocalTime) r.get("endTime");
                LocalTime s2 = ((java.sql.Time) o.get("start_time")).toLocalTime();
                LocalTime e2 = ((java.sql.Time) o.get("end_time")).toLocalTime();
                conflict = s1 != null && e1 != null && s1.isBefore(e2) && s2.isBefore(e1);
            } else if ("HOURS".equals(unit) || "HOURS".equals(ou)) {
                conflict = true;
            }
            if (conflict) {
                throw BusinessException.conflict("LEAVE_OVERLAP", "Trùng với đơn nghỉ khác (#" + o.get("id") + ") trong cùng ngày/ca");
            }
        }
    }

    // ---------------- truy vấn ----------------

    private Map<String, Object> lockOwn(long id, long employeeId) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM leave_request WHERE id = ? AND employee_id = ? FOR UPDATE", id, employeeId);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đơn nghỉ");
        }
        Map<String, Object> r = Sql.camel(rows.get(0));
        r.putAll(rows.get(0));
        return r;
    }

    private void requireWorking(long employeeId) {
        String status = jdbc.queryForObject("SELECT status FROM employee WHERE id = ?", String.class, employeeId);
        if ("TERMINATED".equals(status)) {
            throw BusinessException.conflict("INVALID_STATE", "Nhân viên đã nghỉ việc");
        }
    }

    public Map<String, Object> get(long id) {
        Map<String, Object> r = Sql.one(jdbc, "Không tìm thấy đơn nghỉ", SELECT + " WHERE r.id = ?", id);
        AuthUser u = Principals.current();
        long emp = ((Number) r.get("employeeId")).longValue();
        boolean visible = employees.canSeeOperational(u, emp)
                || (u.employeeId() != null && (u.employeeId().equals(asLong(r.get("primaryApproverId"))) || u.employeeId().equals(asLong(r.get("backupApproverId")))));
        if (!visible) {
            throw new ResourceNotFoundException("Không tìm thấy đơn nghỉ");
        }
        return enrich(r);
    }

    private Map<String, Object> enrich(Map<String, Object> r) {
        r.put("days", Sql.list(jdbc, "SELECT leave_date AS date, minutes, day_fraction FROM leave_request_day WHERE leave_request_id = ? ORDER BY leave_date", r.get("id")));
        boolean can = "PENDING".equals(r.get("status")) && Principals.currentOrNull() != null
                && Principals.current().employeeId() != null
                && approvals.canApprove(asLong(r.get("employeeId")), asLong(r.get("primaryApproverId")), asLong(r.get("backupApproverId")));
        r.put("canApprove", can);
        return r;
    }

    private List<Map<String, Object>> enrichAll(List<Map<String, Object>> rows) {
        rows.forEach(this::enrich);
        return rows;
    }

    public List<Map<String, Object>> mine() {
        return enrichAll(Sql.list(jdbc, SELECT + " WHERE r.employee_id = ? ORDER BY r.id DESC", Principals.employeeId()));
    }

    public List<Map<String, Object>> inbox() {
        long me = Principals.employeeId();
        List<Map<String, Object>> rows = Sql.list(jdbc, SELECT + """
                 WHERE r.status = 'PENDING' AND r.employee_id <> ? AND (r.primary_approver_id = ? OR r.backup_approver_id = ?)
                 ORDER BY r.submitted_at
                """, me, me, me);
        return enrichAll(rows).stream().filter(r -> Boolean.TRUE.equals(r.get("canApprove"))).toList();
    }

    public List<Map<String, Object>> all(String status, Long employeeId) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (u.is("MANAGER")) {
            where.append(" AND e.manager_id = ?");
            args.add(u.employeeId());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND r.status = ?");
            args.add(status);
        }
        if (employeeId != null) {
            where.append(" AND r.employee_id = ?");
            args.add(employeeId);
        }
        return enrichAll(Sql.list(jdbc, SELECT + where + " ORDER BY r.id DESC LIMIT 500", args.toArray()));
    }

    public List<Map<String, Object>> logs(long id) {
        get(id);
        return Sql.list(jdbc, """
                SELECT id, action, actor_name, from_status, to_status, reason, used_backup, created_at
                FROM approval_log WHERE entity_type = 'LEAVE_REQUEST' AND entity_id = ? ORDER BY id
                """, id);
    }

    // ---------------- quỹ phép ----------------

    private Map<String, Object> balanceView(long employeeId, int year) {
        Map<String, Object> emp = jdbc.queryForMap("SELECT code, full_name FROM employee WHERE id = ?", employeeId);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM leave_balance WHERE employee_id = ? AND balance_year = ?", employeeId, year);
        Map<String, Object> out = new LinkedHashMap<>();
        BigDecimal opening = BigDecimal.ZERO, accrued = BigDecimal.ZERO, used = BigDecimal.ZERO, adjusted = BigDecimal.ZERO;
        Long id = null;
        List<Map<String, Object>> history = List.of();
        if (!rows.isEmpty()) {
            Map<String, Object> b = rows.get(0);
            id = ((Number) b.get("id")).longValue();
            opening = (BigDecimal) b.get("opening_days");
            accrued = (BigDecimal) b.get("accrued_days");
            used = (BigDecimal) b.get("used_days");
            adjusted = (BigDecimal) b.get("adjusted_days");
            history = Sql.list(jdbc, "SELECT id, txn_type, delta_days, leave_request_id, reason, created_at FROM leave_balance_txn WHERE balance_id = ? ORDER BY id DESC", id);
        }
        BigDecimal pending = pendingDays(employeeId, year, null);
        out.put("id", id);
        out.put("employeeId", employeeId);
        out.put("employeeCode", emp.get("code"));
        out.put("employeeName", emp.get("full_name"));
        out.put("year", year);
        out.put("openingDays", opening);
        out.put("accruedDays", accrued);
        out.put("usedDays", used);
        out.put("adjustedDays", adjusted);
        out.put("pendingDays", pending);
        out.put("remainingDays", opening.add(accrued).add(adjusted).subtract(used).subtract(pending));
        out.put("history", history);
        return out;
    }

    public Map<String, Object> myBalance(Integer year) {
        return balanceView(Principals.employeeId(), year == null ? Vn.today().getYear() : year);
    }

    public List<Map<String, Object>> balances(Long employeeId, Integer year) {
        Principals.requireRole("HR");
        int y = year == null ? Vn.today().getYear() : year;
        List<Long> ids = employeeId != null ? List.of(employeeId)
                : jdbc.queryForList("SELECT id FROM employee WHERE status <> 'TERMINATED' ORDER BY code", Long.class);
        return ids.stream().map(id -> balanceView(id, y)).toList();
    }

    @Transactional
    public Map<String, Object> adjustBalance(long balanceIdOrEmployee, BigDecimal delta, String reason) {
        Principals.requireRole("HR");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT id, employee_id, balance_year FROM leave_balance WHERE id = ? FOR UPDATE", balanceIdOrEmployee);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy quỹ phép");
        }
        Map<String, Object> b = rows.get(0);
        if (delta.signum() == 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Số ngày điều chỉnh phải khác 0");
        }
        jdbc.update("UPDATE leave_balance SET adjusted_days = adjusted_days + ?, version = version + 1 WHERE id = ?", delta, b.get("id"));
        jdbc.update("INSERT INTO leave_balance_txn(balance_id, txn_type, delta_days, reason, created_by) VALUES (?, 'ADJUST', ?, ?, ?)",
                b.get("id"), delta, reason, Principals.current().userId());
        audit.success("ADJUST_LEAVE_BALANCE", "leave_balance", b.get("id"), reason, null, Map.of("delta", delta));
        return balanceView(((Number) b.get("employee_id")).longValue(), ((Number) b.get("balance_year")).intValue());
    }

    private static Long asLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }
}
