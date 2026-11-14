package com.hrflow.overtime;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.attendance.WorkCalendar;
import com.hrflow.attendance.WorkCalendar.DayPlan;
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
import com.hrflow.payroll.ParameterService;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/**
 * Làm thêm giờ (nhóm E): công bố lịch -> nhân viên đồng ý/đăng ký -> duyệt một cấp -> làm thực tế -> xác nhận.
 * Trả theo giờ THỰC TẾ đủ căn cứ (min(thực tế, đã duyệt) + phần vượt có xác nhận riêng).
 * Giới hạn kiểm soát lấy từ payroll_parameter: OT_MAX_HOURS_DAY / MONTH / YEAR (4h/ngày, 40h/tháng, 200h/năm).
 * Admin không lập lịch OT; Manager chỉ lập lịch cho team trực tiếp.
 */
@Service
public class OvertimeService {

    private final JdbcTemplate jdbc;
    private final WorkCalendar calendar;
    private final ParameterService params;
    private final Approvals approvals;
    private final PeriodGuard guard;
    private final AuditService audit;
    private final ApprovalLogService approvalLog;
    private final NotificationService notifications;
    private final EmployeeService employees;

    public OvertimeService(JdbcTemplate jdbc, WorkCalendar calendar, ParameterService params, Approvals approvals,
                           PeriodGuard guard, AuditService audit, ApprovalLogService approvalLog,
                           NotificationService notifications, EmployeeService employees) {
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.params = params;
        this.approvals = approvals;
        this.guard = guard;
        this.audit = audit;
        this.approvalLog = approvalLog;
        this.notifications = notifications;
        this.employees = employees;
    }

    // ---------------- giới hạn OT ----------------

    /** Phút OT đã cam kết của nhân viên trong khoảng ngày (đăng ký chờ/đã duyệt + OT ngoài lịch chờ/đã xác minh). */
    int committedMinutes(long employeeId, LocalDate from, LocalDate to, Long excludeRegistration, Long excludeUnscheduled) {
        Integer reg = jdbc.queryForObject("""
                SELECT COALESCE(SUM(CASE
                         WHEN r.actual_status = 'CONFIRMED' THEN COALESCE(r.actual_minutes, 0)
                         WHEN r.actual_status = 'NOT_PERFORMED' THEN 0
                         WHEN r.status = 'APPROVED' THEN COALESCE(r.approved_minutes, r.registered_minutes)
                         ELSE r.registered_minutes END), 0)::int
                FROM ot_registration r JOIN ot_schedule s ON s.id = r.schedule_id
                WHERE r.employee_id = ? AND r.status IN ('PENDING', 'APPROVED') AND s.work_date BETWEEN ? AND ?
                  AND (?::bigint IS NULL OR r.id <> ?)
                """, Integer.class, employeeId, from, to, excludeRegistration, excludeRegistration);
        Integer uns = jdbc.queryForObject("""
                SELECT COALESCE(SUM(minutes), 0)::int FROM ot_unscheduled WHERE employee_id = ? AND status IN ('PENDING', 'CONFIRMED')
                  AND work_date BETWEEN ? AND ? AND (?::bigint IS NULL OR id <> ?)
                """, Integer.class, employeeId, from, to, excludeUnscheduled, excludeUnscheduled);
        return (reg == null ? 0 : reg) + (uns == null ? 0 : uns);
    }

    void checkLimits(long employeeId, LocalDate date, int addMinutes, Long excludeReg, Long excludeUns) {
        int dayMax = params.value("OT_MAX_HOURS_DAY", date).multiply(java.math.BigDecimal.valueOf(60)).intValue();
        int monthMax = params.value("OT_MAX_HOURS_MONTH", date).multiply(java.math.BigDecimal.valueOf(60)).intValue();
        int yearMax = params.value("OT_MAX_HOURS_YEAR", date).multiply(java.math.BigDecimal.valueOf(60)).intValue();
        int day = committedMinutes(employeeId, date, date, excludeReg, excludeUns) + addMinutes;
        if (day > dayMax) {
            throw BusinessException.unprocessable("OT_LIMIT_DAY", "Vượt giới hạn OT trong ngày (" + dayMax / 60 + " giờ)");
        }
        LocalDate ms = date.withDayOfMonth(1);
        int month = committedMinutes(employeeId, ms, ms.plusMonths(1).minusDays(1), excludeReg, excludeUns) + addMinutes;
        if (month > monthMax) {
            throw BusinessException.unprocessable("OT_LIMIT_MONTH", "Vượt giới hạn OT trong tháng (" + monthMax / 60 + " giờ)");
        }
        LocalDate ys = LocalDate.of(date.getYear(), 1, 1);
        int year = committedMinutes(employeeId, ys, LocalDate.of(date.getYear(), 12, 31), excludeReg, excludeUns) + addMinutes;
        if (year > yearMax) {
            throw BusinessException.unprocessable("OT_LIMIT_YEAR", "Vượt giới hạn OT trong năm (" + yearMax / 60 + " giờ)");
        }
    }

    private void checkNotOverlappingSchedule(long employeeId, LocalDate date, LocalTime start, LocalTime end) {
        DayPlan plan = calendar.plan(employeeId, date);
        for (WorkCalendar.Interval iv : plan.intervals()) {
            if (start.isBefore(iv.end()) && iv.start().isBefore(end)) {
                throw BusinessException.unprocessable("OT_OVERLAPS_SCHEDULE",
                        "Khung giờ OT trùng giờ làm việc theo lịch của nhân viên (đã được trả ở lương theo công)");
            }
        }
    }

    // ---------------- lịch OT ----------------

    private static final String SCHEDULE_SELECT = """
            SELECT s.id, s.title, s.work_date, s.start_time, s.end_time, s.description, s.department_id, d.name AS department_name,
                   s.status, COALESCE(ce.full_name, ca.email) AS created_by_name, s.published_at,
                   (SELECT COUNT(*) FROM ot_registration r WHERE r.schedule_id = s.id AND r.status <> 'CANCELLED') AS registrations
            FROM ot_schedule s LEFT JOIN department d ON d.id = s.department_id
            JOIN user_account ca ON ca.id = s.created_by LEFT JOIN employee ce ON ce.id = ca.employee_id
            """;

    public record ScheduleCmd(String title, LocalDate workDate, LocalTime startTime, LocalTime endTime,
                              String description, Long departmentId, List<Long> targetEmployeeIds) {
    }

    @Transactional
    public Map<String, Object> createSchedule(ScheduleCmd c) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        if (!c.endTime().isAfter(c.startTime())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Giờ kết thúc OT phải sau giờ bắt đầu (cùng ngày)");
        }
        if (c.workDate().isBefore(Vn.today())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Không lập lịch OT cho ngày đã qua");
        }
        if (c.targetEmployeeIds() == null || c.targetEmployeeIds().isEmpty()) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Phải chọn nhân viên được mời OT");
        }
        guard.assertOpen(c.workDate());
        int minutes = (int) Duration.between(c.startTime(), c.endTime()).toMinutes();
        for (Long emp : c.targetEmployeeIds().stream().distinct().toList()) {
            String status = jdbc.query("SELECT status FROM employee WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, emp);
            if (status == null || !"ACTIVE".equals(status) && !"ON_LEAVE".equals(status)) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "Nhân viên #" + emp + " không còn làm việc");
            }
            if (u.is("MANAGER") && !u.is("HR") && !employees.isTeamMember(u.employeeId(), emp)) {
                throw BusinessException.forbidden("OUT_OF_TEAM", "Manager chỉ lập lịch OT cho nhân viên thuộc team trực tiếp");
            }
            checkNotOverlappingSchedule(emp, c.workDate(), c.startTime(), c.endTime());
            checkLimits(emp, c.workDate(), minutes, null, null);
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO ot_schedule(title, work_date, start_time, end_time, description, department_id, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, 'DRAFT', ?) RETURNING id
                """, Long.class, c.title(), c.workDate(), c.startTime(), c.endTime(), c.description(), c.departmentId(), u.userId());
        for (Long emp : c.targetEmployeeIds().stream().distinct().toList()) {
            jdbc.update("INSERT INTO ot_schedule_target(schedule_id, employee_id) VALUES (?, ?)", id, emp);
        }
        approvalLog.log("OT_SCHEDULE", id, "CREATE", null, "DRAFT", null, false);
        return scheduleView(id);
    }

    private Map<String, Object> scheduleView(long id) {
        Map<String, Object> s = Sql.one(jdbc, "Không tìm thấy lịch OT", SCHEDULE_SELECT + " WHERE s.id = ?", id);
        return enrichSchedule(s);
    }

    private Map<String, Object> enrichSchedule(Map<String, Object> s) {
        List<Map<String, Object>> targets = Sql.list(jdbc, """
                SELECT e.id, e.full_name FROM ot_schedule_target t JOIN employee e ON e.id = t.employee_id WHERE t.schedule_id = ? ORDER BY e.code
                """, s.get("id"));
        s.put("targetEmployeeIds", targets.stream().map(t -> t.get("id")).toList());
        s.put("targetNames", targets.stream().map(t -> t.get("fullName")).toList());
        LocalDate d = (LocalDate) s.get("workDate");
        Integer holiday = jdbc.queryForObject("SELECT COUNT(*) FROM holiday WHERE holiday_date = ? AND paid", Integer.class, d);
        s.put("dayType", holiday != null && holiday > 0 ? "HOLIDAY"
                : d.getDayOfWeek().getValue() >= 6 ? "WEEKLY_OFF" : "WORKDAY");
        Long me = Principals.currentOrNull() == null ? null : Principals.current().employeeId();
        s.put("registeredByMe", me != null && Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM ot_registration WHERE schedule_id = ? AND employee_id = ? AND status <> 'CANCELLED')",
                Boolean.class, s.get("id"), me)));
        return s;
    }

    public List<Map<String, Object>> schedules() {
        AuthUser u = Principals.current();
        if (u.employeeId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Admin không truy cập lịch OT");
        }
        List<Map<String, Object>> rows;
        if (u.is("HR")) {
            rows = Sql.list(jdbc, SCHEDULE_SELECT + " ORDER BY s.work_date DESC, s.id DESC LIMIT 500");
        } else if (u.is("MANAGER")) {
            rows = Sql.list(jdbc, SCHEDULE_SELECT + """
                     WHERE s.created_by = ? OR EXISTS (SELECT 1 FROM ot_schedule_target t JOIN employee e ON e.id = t.employee_id
                           WHERE t.schedule_id = s.id AND (e.manager_id = ? OR e.id = ?))
                     ORDER BY s.work_date DESC, s.id DESC LIMIT 500
                    """, u.userId(), u.employeeId(), u.employeeId());
        } else {
            rows = Sql.list(jdbc, SCHEDULE_SELECT + """
                     WHERE s.status IN ('PUBLISHED', 'CLOSED') AND EXISTS (SELECT 1 FROM ot_schedule_target t WHERE t.schedule_id = s.id AND t.employee_id = ?)
                     ORDER BY s.work_date DESC, s.id DESC
                    """, u.employeeId());
        }
        rows.forEach(this::enrichSchedule);
        return rows;
    }

    private Map<String, Object> lockSchedule(long id) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ot_schedule WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy lịch OT");
        }
        Map<String, Object> s = rows.get(0);
        if (!u.is("HR") && ((Number) s.get("created_by")).longValue() != u.userId()) {
            throw new ResourceNotFoundException("Không tìm thấy lịch OT");
        }
        return s;
    }

    @Transactional
    public Map<String, Object> publish(long id) {
        Map<String, Object> s = lockSchedule(id);
        if (!"DRAFT".equals(s.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ công bố được lịch nháp");
        }
        guard.assertOpen(((java.sql.Date) s.get("work_date")).toLocalDate());
        jdbc.update("UPDATE ot_schedule SET status = 'PUBLISHED', published_at = now(), version = version + 1 WHERE id = ?", id);
        approvalLog.log("OT_SCHEDULE", id, "PUBLISH", "DRAFT", "PUBLISHED", null, false);
        for (Long emp : jdbc.queryForList("SELECT employee_id FROM ot_schedule_target WHERE schedule_id = ?", Long.class, id)) {
            notifications.toEmployee(emp, "OT_PUBLISHED", "Lịch OT mới", s.get("title") + " - bạn được mời đăng ký", "/ot-schedules", "ot-publish:" + id);
        }
        return scheduleView(id);
    }

    @Transactional
    public Map<String, Object> closeSchedule(long id) {
        Map<String, Object> s = lockSchedule(id);
        if (!"PUBLISHED".equals(s.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ đóng được lịch đã công bố");
        }
        jdbc.update("UPDATE ot_schedule SET status = 'CLOSED', version = version + 1 WHERE id = ?", id);
        approvalLog.log("OT_SCHEDULE", id, "CLOSE", "PUBLISHED", "CLOSED", null, false);
        return scheduleView(id);
    }

    @Transactional
    public Map<String, Object> cancelSchedule(long id) {
        Map<String, Object> s = lockSchedule(id);
        if ("CANCELLED".equals(s.get("status")) || "CLOSED".equals(s.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Lịch đã đóng/hủy");
        }
        Integer confirmed = jdbc.queryForObject("SELECT COUNT(*) FROM ot_registration WHERE schedule_id = ? AND actual_status <> 'NOT_RECORDED'", Integer.class, id);
        if (confirmed != null && confirmed > 0) {
            throw BusinessException.conflict("INVALID_STATE", "Lịch đã có giờ thực tế được xác nhận, không thể hủy");
        }
        jdbc.update("UPDATE ot_schedule SET status = 'CANCELLED', version = version + 1 WHERE id = ?", id);
        jdbc.update("UPDATE ot_registration SET status = 'CANCELLED', version = version + 1 WHERE schedule_id = ? AND status IN ('PENDING', 'APPROVED')", id);
        approvalLog.log("OT_SCHEDULE", id, "CANCEL", (String) s.get("status"), "CANCELLED", null, false);
        return scheduleView(id);
    }

    // ---------------- đăng ký OT ----------------

    private static final String REG_SELECT = """
            SELECT r.id, r.schedule_id, s.title AS schedule_title, s.work_date, r.employee_id, e.code AS employee_code,
                   e.full_name AS employee_name, r.consent, r.registered_start, r.registered_end, r.registered_minutes,
                   r.status, r.approved_minutes, pe.full_name AS primary_approver_name, be.full_name AS backup_approver_name,
                   r.primary_approver_id, r.backup_approver_id, de.full_name AS decided_by_name, r.decided_at, r.decision_reason,
                   r.used_backup, r.actual_status, r.actual_start, r.actual_end, r.actual_minutes, r.extra_minutes_confirmed,
                   ae.full_name AS actual_confirmed_by_name, r.actual_evidence, r.hr_checked,
                   CASE WHEN r.actual_status = 'CONFIRMED'
                        THEN LEAST(COALESCE(r.actual_minutes, 0), COALESCE(r.approved_minutes, 0)) + r.extra_minutes_confirmed
                        ELSE 0 END AS payable_minutes
            FROM ot_registration r JOIN ot_schedule s ON s.id = r.schedule_id JOIN employee e ON e.id = r.employee_id
            LEFT JOIN employee pe ON pe.id = r.primary_approver_id LEFT JOIN employee be ON be.id = r.backup_approver_id
            LEFT JOIN employee de ON de.id = r.decided_by LEFT JOIN employee ae ON ae.id = r.actual_confirmed_by
            """;

    private Map<String, Object> regView(long id) {
        return enrichReg(Sql.one(jdbc, "Không tìm thấy đăng ký OT", REG_SELECT + " WHERE r.id = ?", id));
    }

    private Map<String, Object> enrichReg(Map<String, Object> r) {
        AuthUser u = Principals.current();
        long emp = ((Number) r.get("employeeId")).longValue();
        r.put("canApprove", "PENDING".equals(r.get("status")) && u.employeeId() != null
                && approvals.canApprove(emp, asLong(r.get("primaryApproverId")), asLong(r.get("backupApproverId"))));
        r.put("canConfirmActual", "APPROVED".equals(r.get("status")) && u.employeeId() != null && u.employeeId() != emp
                && (u.is("HR") || u.is("MANAGER") && employees.isTeamMember(u.employeeId(), emp))
                && !((LocalDate) r.get("workDate")).isAfter(Vn.today()));
        return r;
    }

    public record RegisterCmd(long scheduleId, boolean consent, LocalTime startTime, LocalTime endTime) {
    }

    @Transactional
    public Map<String, Object> register(RegisterCmd c) {
        long emp = Principals.employeeId();
        if (!c.consent()) {
            throw BusinessException.unprocessable("CONSENT_REQUIRED", "Phải đồng ý làm thêm giờ mới đăng ký được");
        }
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ot_schedule WHERE id = ? FOR UPDATE", c.scheduleId());
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy lịch OT");
        }
        Map<String, Object> s = rows.get(0);
        Integer invited = jdbc.queryForObject("SELECT COUNT(*) FROM ot_schedule_target WHERE schedule_id = ? AND employee_id = ?", Integer.class, c.scheduleId(), emp);
        if (invited == null || invited == 0) {
            throw new ResourceNotFoundException("Không tìm thấy lịch OT");
        }
        if (!"PUBLISHED".equals(s.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Lịch OT chưa công bố hoặc đã đóng");
        }
        LocalDate date = ((java.sql.Date) s.get("work_date")).toLocalDate();
        guard.assertOpen(date);
        LocalTime start = c.startTime() == null ? ((java.sql.Time) s.get("start_time")).toLocalTime() : c.startTime();
        LocalTime end = c.endTime() == null ? ((java.sql.Time) s.get("end_time")).toLocalTime() : c.endTime();
        if (!end.isAfter(start) || start.isBefore(((java.sql.Time) s.get("start_time")).toLocalTime())
                || end.isAfter(((java.sql.Time) s.get("end_time")).toLocalTime())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Khung giờ đăng ký phải nằm trong khung của lịch OT");
        }
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM ot_registration WHERE schedule_id = ? AND employee_id = ?", Integer.class, c.scheduleId(), emp);
        if (dup != null && dup > 0) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Bạn đã đăng ký lịch OT này");
        }
        int minutes = (int) Duration.between(start, end).toMinutes();
        checkNotOverlappingSchedule(emp, date, start, end);
        checkLimits(emp, date, minutes, null, null);
        Approvals.Route route = approvals.routeOf(emp, date);
        Instant rs = LocalDateTime.of(date, start).atZone(Vn.ZONE).toInstant();
        Instant re = LocalDateTime.of(date, end).atZone(Vn.ZONE).toInstant();
        Long id = jdbc.queryForObject("""
                INSERT INTO ot_registration(schedule_id, employee_id, consent, registered_start, registered_end, registered_minutes,
                       status, primary_approver_id, backup_approver_id, actual_status)
                VALUES (?, ?, TRUE, ?, ?, ?, 'PENDING', ?, ?, 'NOT_RECORDED') RETURNING id
                """, Long.class, c.scheduleId(), emp, Timestamp.from(rs), Timestamp.from(re), minutes, route.primaryId(), route.backupId());
        approvalLog.log("OT_REGISTRATION", id, "REGISTER", null, "PENDING", null, false);
        notifications.toEmployee(route.primaryId(), "OT_PENDING", "Đăng ký OT chờ duyệt", "Có đăng ký OT chờ duyệt", "/ot-registrations/inbox", "ot-reg:" + id);
        return regView(id);
    }

    public List<Map<String, Object>> myRegistrations() {
        return Sql.list(jdbc, REG_SELECT + " WHERE r.employee_id = ? ORDER BY r.id DESC", Principals.employeeId())
                .stream().map(this::enrichReg).toList();
    }

    public List<Map<String, Object>> registrations() {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        List<Map<String, Object>> rows = u.is("HR")
                ? Sql.list(jdbc, REG_SELECT + " ORDER BY r.id DESC LIMIT 500")
                : Sql.list(jdbc, REG_SELECT + " WHERE e.manager_id = ? ORDER BY r.id DESC LIMIT 500", u.employeeId());
        return rows.stream().map(this::enrichReg).toList();
    }

    public List<Map<String, Object>> inbox() {
        long me = Principals.employeeId();
        return Sql.list(jdbc, REG_SELECT + """
                 WHERE r.status = 'PENDING' AND r.employee_id <> ? AND (r.primary_approver_id = ? OR r.backup_approver_id = ?)
                 ORDER BY r.id
                """, me, me, me).stream().map(this::enrichReg).filter(r -> Boolean.TRUE.equals(r.get("canApprove"))).toList();
    }

    @Transactional
    public Map<String, Object> decide(long id, boolean approve, Integer approvedMinutes, String reason, String backupReason) {
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ot_registration WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đăng ký OT");
        }
        Map<String, Object> r = rows.get(0);
        long emp = ((Number) r.get("employee_id")).longValue();
        boolean usedBackup = approvals.authorize("OT_REGISTRATION", id, emp, asLong(r.get("primary_approver_id")),
                asLong(r.get("backup_approver_id")), backupReason);
        if (!"PENDING".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Đăng ký không ở trạng thái chờ duyệt");
        }
        if (!approve && (reason == null || reason.isBlank())) {
            throw BusinessException.unprocessable("REASON_REQUIRED", "Từ chối phải ghi lý do");
        }
        LocalDate date = jdbc.queryForObject("SELECT work_date FROM ot_schedule WHERE id = ?", LocalDate.class, r.get("schedule_id"));
        guard.assertOpen(date);
        int registered = ((Number) r.get("registered_minutes")).intValue();
        Integer approved = null;
        if (approve) {
            approved = approvedMinutes == null ? registered : approvedMinutes;
            if (approved <= 0 || approved > registered) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "Số phút duyệt phải từ 1 đến số phút đăng ký");
            }
            checkLimits(emp, date, approved - registered, id, null);
        }
        AuthUser u = Principals.current();
        jdbc.update("""
                UPDATE ot_registration SET status = ?, approved_minutes = ?, decided_by = ?, decided_at = now(), decision_reason = ?,
                       used_backup = ?, version = version + 1 WHERE id = ?
                """, approve ? "APPROVED" : "REJECTED", approved, u.employeeId(), reason, usedBackup, id);
        approvalLog.log("OT_REGISTRATION", id, approve ? "APPROVE" : "REJECT", "PENDING", approve ? "APPROVED" : "REJECTED",
                usedBackup ? "Duyệt thay: " + backupReason + (reason == null ? "" : " | " + reason) : reason, usedBackup);
        notifications.toEmployee(emp, approve ? "OT_APPROVED" : "OT_REJECTED", approve ? "OT được duyệt" : "OT bị từ chối",
                "Đăng ký OT ngày " + date, "/ot-registrations/me", "ot-decision:" + id);
        return regView(id);
    }

    @Transactional
    public Map<String, Object> cancel(long id) {
        long emp = Principals.employeeId();
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ot_registration WHERE id = ? AND employee_id = ? FOR UPDATE", id, emp);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đăng ký OT");
        }
        Map<String, Object> r = rows.get(0);
        if (!"PENDING".equals(r.get("status")) && !"APPROVED".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Đăng ký đã được xử lý");
        }
        if (!"NOT_RECORDED".equals(r.get("actual_status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Đã xác nhận giờ thực tế, không thể hủy");
        }
        guard.assertOpen(jdbc.queryForObject("SELECT work_date FROM ot_schedule WHERE id = ?", LocalDate.class, r.get("schedule_id")));
        jdbc.update("UPDATE ot_registration SET status = 'CANCELLED', version = version + 1 WHERE id = ?", id);
        approvalLog.log("OT_REGISTRATION", id, "CANCEL", (String) r.get("status"), "CANCELLED", null, false);
        return regView(id);
    }

    // ---------------- giờ thực tế ----------------

    public record ActualCmd(String status, Instant actualStart, Instant actualEnd, boolean extraConfirmed, String evidence) {
    }

    /**
     * Xác nhận giờ OT thực tế. Số phút trả = min(thực tế, đã duyệt) + phần vượt (nếu có xác nhận và minh chứng riêng).
     * Người làm OT không tự xác nhận giờ của mình.
     */
    @Transactional
    public Map<String, Object> confirmActual(long id, ActualCmd c) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ot_registration WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đăng ký OT");
        }
        Map<String, Object> r = rows.get(0);
        long emp = ((Number) r.get("employee_id")).longValue();
        if (u.employeeId() != null && u.employeeId() == emp) {
            audit.denied("SELF_APPROVAL", "ot_registration", id, "Tự xác nhận giờ OT thực tế của mình");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không được tự xác nhận giờ OT của mình");
        }
        if (!u.is("HR") && !employees.isTeamMember(u.employeeId(), emp)) {
            throw new ResourceNotFoundException("Không tìm thấy đăng ký OT");
        }
        if (!"APPROVED".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ xác nhận giờ thực tế cho đăng ký đã duyệt");
        }
        LocalDate date = jdbc.queryForObject("SELECT work_date FROM ot_schedule WHERE id = ?", LocalDate.class, r.get("schedule_id"));
        if (date.isAfter(Vn.today())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Chưa tới ngày OT");
        }
        guard.assertOpen(date);
        Integer actualMinutes = null;
        int extra = 0;
        if ("CONFIRMED".equals(c.status())) {
            if (c.actualStart() == null || c.actualEnd() == null || !c.actualEnd().isAfter(c.actualStart())) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "Phải có giờ thực tế bắt đầu/kết thúc hợp lệ");
            }
            actualMinutes = (int) Duration.between(c.actualStart(), c.actualEnd()).toMinutes();
            int approved = ((Number) r.get("approved_minutes")).intValue();
            if (actualMinutes > approved) {
                if (!c.extraConfirmed() || c.evidence() == null || c.evidence().isBlank()) {
                    // Chỉ trả tối đa phần đã duyệt; phần vượt phải có xác nhận + minh chứng riêng.
                    extra = 0;
                } else {
                    extra = actualMinutes - approved;
                }
            }
        } else if (!"NOT_PERFORMED".equals(c.status()) && !"NOT_RECORDED".equals(c.status())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Trạng thái giờ thực tế không hợp lệ");
        }
        jdbc.update("""
                UPDATE ot_registration SET actual_status = ?, actual_start = ?, actual_end = ?, actual_minutes = ?,
                       extra_minutes_confirmed = ?, actual_confirmed_by = ?, actual_confirmed_at = now(), actual_evidence = ?,
                       hr_checked = FALSE, hr_checked_by = NULL, hr_checked_at = NULL, version = version + 1 WHERE id = ?
                """, c.status(), "CONFIRMED".equals(c.status()) ? Timestamp.from(c.actualStart()) : null,
                "CONFIRMED".equals(c.status()) ? Timestamp.from(c.actualEnd()) : null, actualMinutes, extra, u.employeeId(), c.evidence(), id);
        audit.success("CONFIRM_OT_ACTUAL", "ot_registration", id, c.evidence(), null,
                Map.of("status", c.status(), "actualMinutes", String.valueOf(actualMinutes), "extra", extra));
        return regView(id);
    }

    @Transactional
    public Map<String, Object> hrCheck(long id) {
        AuthUser hr = Principals.requireRole("HR");
        Map<String, Object> r = Sql.one(jdbc, "Không tìm thấy đăng ký OT", "SELECT employee_id, actual_status FROM ot_registration WHERE id = ?", id);
        if (hr.employeeId() != null && hr.employeeId().equals(asLong(r.get("employeeId")))) {
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không tự đối chiếu giờ OT của mình");
        }
        if (!"CONFIRMED".equals(r.get("actualStatus"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chưa có giờ thực tế được xác nhận để đối chiếu");
        }
        jdbc.update("UPDATE ot_registration SET hr_checked = TRUE, hr_checked_by = ?, hr_checked_at = now(), version = version + 1 WHERE id = ?", hr.userId(), id);
        return regView(id);
    }

    // ---------------- OT ngoài lịch ----------------

    private static final String UNS_SELECT = """
            SELECT o.id, o.employee_id, e.code AS employee_code, e.full_name AS employee_name, o.work_date, o.start_at, o.end_at,
                   o.minutes, o.description, o.evidence, o.status, COALESCE(ce.full_name, ca.email) AS created_by_name,
                   COALESCE(ve.full_name, va.email) AS verified_by_name, o.verified_at, o.verify_note, o.created_by
            FROM ot_unscheduled o JOIN employee e ON e.id = o.employee_id
            JOIN user_account ca ON ca.id = o.created_by LEFT JOIN employee ce ON ce.id = ca.employee_id
            LEFT JOIN user_account va ON va.id = o.verified_by LEFT JOIN employee ve ON ve.id = va.employee_id
            """;

    private Map<String, Object> unsView(long id) {
        return enrichUns(Sql.one(jdbc, "Không tìm thấy OT ngoài lịch", UNS_SELECT + " WHERE o.id = ?", id));
    }

    private Map<String, Object> enrichUns(Map<String, Object> o) {
        AuthUser u = Principals.current();
        long emp = ((Number) o.get("employeeId")).longValue();
        o.put("canVerify", "PENDING".equals(o.get("status")) && u.employeeId() != null && u.employeeId() != emp
                && ((Number) o.get("createdBy")).longValue() != u.userId()
                && (u.is("HR") || u.is("MANAGER") && employees.isTeamMember(u.employeeId(), emp)));
        return o;
    }

    @Transactional
    public Map<String, Object> createUnscheduled(Long employeeId, Instant startAt, Instant endAt, String description, String evidence) {
        AuthUser u = Principals.current();
        long mine = Principals.employeeId();
        long emp = employeeId == null ? mine : employeeId;
        if (emp != mine && !u.is("HR") && !(u.is("MANAGER") && employees.isTeamMember(mine, emp))) {
            throw BusinessException.forbidden("OUT_OF_TEAM", "Chỉ ghi nhận OT ngoài lịch cho mình hoặc nhân viên thuộc phạm vi quản lý");
        }
        if (!endAt.isAfter(startAt) || !Vn.dateOf(startAt).equals(Vn.dateOf(endAt.minusSeconds(1)))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Khoảng OT phải hợp lệ và nằm trong một ngày");
        }
        LocalDate date = Vn.dateOf(startAt);
        if (date.isAfter(Vn.today())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Không ghi nhận OT cho ngày trong tương lai");
        }
        guard.assertOpen(date);
        int minutes = (int) Duration.between(startAt, endAt).toMinutes();
        checkLimits(emp, date, minutes, null, null);
        Long id = jdbc.queryForObject("""
                INSERT INTO ot_unscheduled(employee_id, work_date, start_at, end_at, minutes, description, evidence, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, 'PENDING', ?) RETURNING id
                """, Long.class, emp, date, Timestamp.from(startAt), Timestamp.from(endAt), minutes, description, evidence, u.userId());
        return unsView(id);
    }

    public List<Map<String, Object>> unscheduled() {
        AuthUser u = Principals.current();
        if (u.employeeId() == null) {
            throw new org.springframework.security.access.AccessDeniedException("Admin không truy cập OT");
        }
        List<Map<String, Object>> rows;
        if (u.is("HR")) {
            rows = Sql.list(jdbc, UNS_SELECT + " ORDER BY o.id DESC LIMIT 500");
        } else if (u.is("MANAGER")) {
            rows = Sql.list(jdbc, UNS_SELECT + " WHERE e.manager_id = ? OR o.employee_id = ? ORDER BY o.id DESC", u.employeeId(), u.employeeId());
        } else {
            rows = Sql.list(jdbc, UNS_SELECT + " WHERE o.employee_id = ? ORDER BY o.id DESC", u.employeeId());
        }
        return rows.stream().map(this::enrichUns).toList();
    }

    @Transactional
    public Map<String, Object> verifyUnscheduled(long id, boolean confirm, String note) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM ot_unscheduled WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy OT ngoài lịch");
        }
        Map<String, Object> o = rows.get(0);
        long emp = ((Number) o.get("employee_id")).longValue();
        if ((u.employeeId() != null && u.employeeId() == emp) || ((Number) o.get("created_by")).longValue() == u.userId()) {
            audit.denied("SELF_APPROVAL", "ot_unscheduled", id, "Người tạo/người làm tự xác minh OT ngoài lịch");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Người tạo/người làm OT không được tự xác minh");
        }
        if (!u.is("HR") && !employees.isTeamMember(u.employeeId(), emp)) {
            throw new ResourceNotFoundException("Không tìm thấy OT ngoài lịch");
        }
        if (!"PENDING".equals(o.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "OT đã được xác minh");
        }
        if (!confirm && (note == null || note.isBlank())) {
            throw BusinessException.unprocessable("REASON_REQUIRED", "Từ chối phải ghi lý do");
        }
        guard.assertOpen(((java.sql.Date) o.get("work_date")).toLocalDate());
        jdbc.update("UPDATE ot_unscheduled SET status = ?, verified_by = ?, verified_at = now(), verify_note = ? WHERE id = ?",
                confirm ? "CONFIRMED" : "REJECTED", u.userId(), note, id);
        audit.success(confirm ? "CONFIRM_OT_UNSCHEDULED" : "REJECT_OT_UNSCHEDULED", "ot_unscheduled", id, note, null, null);
        return unsView(id);
    }

    private static Long asLong(Object o) {
        return o == null ? null : ((Number) o).longValue();
    }
}
