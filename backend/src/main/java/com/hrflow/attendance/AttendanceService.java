package com.hrflow.attendance;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.attendance.AttendanceCalc.Result;
import com.hrflow.attendance.WorkCalendar.DayPlan;
import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.util.Json;
import com.hrflow.common.util.Sql;
import com.hrflow.common.util.Vn;
import com.hrflow.hr.EmployeeService;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

@Service
public class AttendanceService {

    static final String QR_PREFIX = "HRFLOW-QR:";
    private static final Set<String> METHODS = Set.of("QR", "MANUAL", "BUSINESS_TRIP", "ABSENCE");

    static final String SELECT = """
            SELECT a.id, a.employee_id, e.code AS employee_code, e.full_name AS employee_name, a.work_date,
                   a.check_in_at, a.check_out_at, pi.name AS check_in_point, po.name AS check_out_point,
                   (a.check_in_photo_id IS NOT NULL) AS has_check_in_photo, (a.check_out_photo_id IS NOT NULL) AS has_check_out_photo,
                   (a.check_in_nonce IS NOT NULL) AS qr_verified_in, (a.check_out_nonce IS NOT NULL) AS qr_verified_out,
                   a.method, a.scheduled_minutes, a.worked_minutes, a.late_minutes, a.early_minutes,
                   CASE WHEN a.status = 'VALID' AND a.work_date < current_date AND (a.check_in_at IS NULL OR a.check_out_at IS NULL)
                        THEN 'NEEDS_REVIEW' ELSE a.status END AS status,
                   COALESCE(a.review_reason, CASE WHEN a.status = 'VALID' AND a.work_date < current_date AND a.check_out_at IS NULL
                        THEN 'Thiếu check-out' END) AS review_reason,
                   COALESCE(ce.full_name, ca.email) AS confirmed_by_name, a.confirmed_at, a.note
            FROM attendance_record a JOIN employee e ON e.id = a.employee_id
            LEFT JOIN attendance_point pi ON pi.id = a.check_in_point_id LEFT JOIN attendance_point po ON po.id = a.check_out_point_id
            LEFT JOIN user_account ca ON ca.id = a.confirmed_by LEFT JOIN employee ce ON ce.id = ca.employee_id
            """;

    private final JdbcTemplate jdbc;
    private final WorkCalendar calendar;
    private final TimesheetService timesheets;
    private final PeriodGuard guard;
    private final AuditService audit;
    private final EmployeeService employees;
    private final Json json;
    private final boolean requireQr;
    private final int qrTtl;
    private final int photoMaxKb;

    public AttendanceService(JdbcTemplate jdbc, WorkCalendar calendar, TimesheetService timesheets, PeriodGuard guard,
                             AuditService audit, EmployeeService employees, Json json,
                             @Value("${hrflow.attendance.require-qr:false}") boolean requireQr,
                             @Value("${hrflow.attendance.qr-ttl-seconds:15}") int qrTtl,
                             @Value("${hrflow.attendance.photo-max-kb:200}") int photoMaxKb) {
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.timesheets = timesheets;
        this.guard = guard;
        this.audit = audit;
        this.employees = employees;
        this.json = json;
        this.requireQr = requireQr;
        this.qrTtl = qrTtl;
        this.photoMaxKb = photoMaxKb;
    }

    // ---------------- check-in / check-out ----------------

    public record Scan(String qrPayload, String shortCode, String photoBase64, String photoMime) {
    }

    private record Verified(Long pointId, String nonce, Long photoId) {
    }

    private Verified verify(Scan scan, long employeeId) {
        Long pointId = null;
        String nonce = null;
        boolean hasQr = scan != null && ((scan.qrPayload() != null && !scan.qrPayload().isBlank())
                || (scan.shortCode() != null && !scan.shortCode().isBlank()));
        if (hasQr) {
            List<Map<String, Object>> rows;
            if (scan.qrPayload() != null && !scan.qrPayload().isBlank()) {
                String p = scan.qrPayload().trim();
                if (!p.startsWith(QR_PREFIX)) {
                    throw BusinessException.unprocessable("QR_INVALID", "Mã QR không hợp lệ");
                }
                rows = jdbc.queryForList("SELECT point_id, nonce, expires_at FROM qr_token WHERE nonce = ?", p.substring(QR_PREFIX.length()));
            } else {
                rows = jdbc.queryForList("""
                        SELECT point_id, nonce, expires_at FROM qr_token WHERE short_code = ? AND expires_at > now()
                        ORDER BY issued_at DESC LIMIT 1
                        """, scan.shortCode().trim());
            }
            if (rows.isEmpty()) {
                throw BusinessException.unprocessable("QR_INVALID", "Mã QR không hợp lệ");
            }
            Map<String, Object> t = rows.get(0);
            if (((Timestamp) t.get("expires_at")).toInstant().isBefore(Instant.now())) {
                throw BusinessException.unprocessable("QR_EXPIRED", "Mã QR đã hết hạn, hãy quét lại mã mới");
            }
            pointId = ((Number) t.get("point_id")).longValue();
            nonce = (String) t.get("nonce");
        } else if (requireQr) {
            throw BusinessException.unprocessable("QR_INVALID", "Điểm chấm công yêu cầu quét mã QR");
        }
        Long photoId = null;
        boolean photoRequired = pointId != null && Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT photo_required FROM attendance_point WHERE id = ?", Boolean.class, pointId));
        if (scan != null && scan.photoBase64() != null && !scan.photoBase64().isBlank()) {
            byte[] bytes;
            try {
                bytes = Base64.getDecoder().decode(scan.photoBase64());
            } catch (IllegalArgumentException ex) {
                throw BusinessException.unprocessable("PHOTO_INVALID", "Ảnh minh chứng không hợp lệ");
            }
            if (bytes.length > photoMaxKb * 1024L) {
                throw BusinessException.unprocessable("PHOTO_TOO_LARGE", "Ảnh minh chứng tối đa " + photoMaxKb + "KB");
            }
            KeyHolder kh = new GeneratedKeyHolder();
            final byte[] content = bytes;
            final String mime = scan.photoMime() == null ? "image/jpeg" : scan.photoMime();
            jdbc.update(con -> {
                var ps = con.prepareStatement("INSERT INTO attendance_photo(employee_id, content, mime_type, size_bytes) VALUES (?, ?, ?, ?)",
                        new String[] {"id"});
                ps.setLong(1, employeeId);
                ps.setBytes(2, content);
                ps.setString(3, mime);
                ps.setInt(4, content.length);
                return ps;
            }, kh);
            photoId = kh.getKey().longValue();
        } else if (photoRequired) {
            throw BusinessException.unprocessable("PHOTO_REQUIRED", "Điểm chấm công yêu cầu ảnh minh chứng");
        }
        return new Verified(pointId, nonce, photoId);
    }

    @Transactional
    public Map<String, Object> checkIn(Scan scan) {
        long emp = Principals.employeeId();
        requireWorking(emp);
        LocalDate today = Vn.today();
        guard.assertOpen(today);
        Verified v = verify(scan, emp);
        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT id, check_in_at, method FROM attendance_record WHERE employee_id = ? AND work_date = ? FOR UPDATE", emp, today);
        if (!existing.isEmpty()) {
            throw BusinessException.conflict("ALREADY_CHECKED_IN", "Hôm nay bạn đã có bản ghi chấm công");
        }
        Instant now = Instant.now();
        DayPlan plan = calendar.plan(emp, today);
        String method = v.pointId() != null ? "QR" : "MANUAL";
        Result r = AttendanceCalc.compute(plan, now, null, method, today);
        Long id = jdbc.queryForObject("""
                INSERT INTO attendance_record(employee_id, work_date, check_in_at, check_in_point_id, check_in_nonce, check_in_photo_id,
                       method, scheduled_minutes, worked_minutes, late_minutes, early_minutes, status, review_reason)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, emp, today, Timestamp.from(now), v.pointId(), v.nonce(), v.photoId(), method,
                r.scheduledMinutes(), r.workedMinutes(), r.lateMinutes(), r.earlyMinutes(), r.status(), r.reviewReason());
        return get(id);
    }

    @Transactional
    public Map<String, Object> checkOut(Scan scan) {
        long emp = Principals.employeeId();
        requireWorking(emp);
        LocalDate today = Vn.today();
        guard.assertOpen(today);
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id, check_in_at, check_out_at, method FROM attendance_record
                WHERE employee_id = ? AND work_date = ? FOR UPDATE
                """, emp, today);
        if (rows.isEmpty() || rows.get(0).get("check_in_at") == null || !"QR".equals(rows.get(0).get("method")) && !"MANUAL".equals(rows.get(0).get("method"))) {
            throw BusinessException.conflict("NO_CHECK_IN", "Chưa check-in hôm nay");
        }
        Map<String, Object> rec = rows.get(0);
        if (rec.get("check_out_at") != null) {
            throw BusinessException.conflict("ALREADY_CHECKED_OUT", "Hôm nay đã check-out");
        }
        Verified v = verify(scan, emp);
        Instant now = Instant.now();
        Instant in = ((Timestamp) rec.get("check_in_at")).toInstant();
        Result r = AttendanceCalc.compute(calendar.plan(emp, today), in, now, (String) rec.get("method"), today);
        jdbc.update("""
                UPDATE attendance_record SET check_out_at = ?, check_out_point_id = ?, check_out_nonce = ?, check_out_photo_id = ?,
                       scheduled_minutes = ?, worked_minutes = ?, late_minutes = ?, early_minutes = ?, status = ?,
                       review_reason = ?, version = version + 1 WHERE id = ?
                """, Timestamp.from(now), v.pointId(), v.nonce(), v.photoId(), r.scheduledMinutes(), r.workedMinutes(),
                r.lateMinutes(), r.earlyMinutes(), r.status(), r.reviewReason(), rec.get("id"));
        return get(((Number) rec.get("id")).longValue());
    }

    private void requireWorking(long employeeId) {
        String status = jdbc.queryForObject("SELECT status FROM employee WHERE id = ?", String.class, employeeId);
        if ("TERMINATED".equals(status) || "ONBOARDING".equals(status)) {
            throw BusinessException.conflict("INVALID_STATE", "Nhân viên chưa/không còn làm việc nên không chấm công");
        }
    }

    public Map<String, Object> today() {
        long emp = Principals.employeeId();
        List<Map<String, Object>> rows = Sql.list(jdbc, SELECT + " WHERE a.employee_id = ? AND a.work_date = ?", emp, Vn.today());
        return rows.isEmpty() ? null : rows.get(0);
    }

    // ---------------- truy vấn ----------------

    /** own / team / company; Admin không truy cập dữ liệu chấm công. */
    private void requireAttendanceViewer(AuthUser u) {
        if (u.employeeId() == null) {
            throw new AccessDeniedException("Tài khoản không gắn hồ sơ nhân viên");
        }
    }

    public Map<String, Object> get(long id) {
        Map<String, Object> rec = Sql.one(jdbc, "Không tìm thấy bản ghi chấm công", SELECT + " WHERE a.id = ?", id);
        employees.requireOperationalScope(((Number) rec.get("employeeId")).longValue());
        return rec;
    }

    public List<Map<String, Object>> list(Long employeeId, LocalDate from, LocalDate to, String status) {
        AuthUser u = Principals.current();
        requireAttendanceViewer(u);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (employeeId != null) {
            employees.requireOperationalScope(employeeId);
            where.append(" AND a.employee_id = ?");
            args.add(employeeId);
        } else if (u.is("EMPLOYEE")) {
            where.append(" AND a.employee_id = ?");
            args.add(u.employeeId());
        } else if (u.is("MANAGER")) {
            where.append(" AND (a.employee_id = ? OR e.manager_id = ?)");
            args.add(u.employeeId());
            args.add(u.employeeId());
        }
        if (from != null) {
            where.append(" AND a.work_date >= ?");
            args.add(from);
        }
        if (to != null) {
            where.append(" AND a.work_date <= ?");
            args.add(to);
        }
        String sql = SELECT + where + " ORDER BY a.work_date DESC, e.code LIMIT 1000";
        List<Map<String, Object>> rows = Sql.list(jdbc, sql, args.toArray());
        if (status != null && !status.isBlank()) {
            rows = rows.stream().filter(r -> status.equals(r.get("status"))).toList();
        }
        return rows;
    }

    /** Bản ghi cần rà soát + lỗi chặn chốt công trong tháng, trong phạm vi HR (toàn công ty) hoặc Manager (team). */
    public List<Map<String, Object>> review(int year, int month) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        List<Long> ids = u.is("HR")
                ? jdbc.queryForList("SELECT id FROM employee WHERE hire_date <= ? AND (termination_date IS NULL OR termination_date >= ?) ORDER BY code", Long.class, to, from)
                : jdbc.queryForList("SELECT id FROM employee WHERE manager_id = ? AND hire_date <= ? AND (termination_date IS NULL OR termination_date >= ?) ORDER BY code", Long.class, u.employeeId(), to, from);
        List<Map<String, Object>> out = new ArrayList<>();
        for (Long id : ids) {
            Map<String, Object> e = jdbc.queryForMap("SELECT code, full_name FROM employee WHERE id = ?", id);
            for (TimesheetService.Day d : timesheets.days(id, from, to)) {
                if (d.issue() != null) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("employeeId", id);
                    row.put("employeeCode", e.get("code"));
                    row.put("employeeName", e.get("full_name"));
                    row.put("date", d.date());
                    row.put("attendanceId", d.attendanceId());
                    row.put("level", d.issueLevel());
                    row.put("issue", d.issue());
                    out.add(row);
                }
            }
        }
        return out;
    }

    public Map<String, Object> timesheet(Long employeeId, int year, int month) {
        AuthUser u = Principals.current();
        requireAttendanceViewer(u);
        long target = employeeId == null ? u.employeeId() : employeeId;
        employees.requireOperationalScope(target);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1).minusDays(1);
        Map<String, Object> e = jdbc.queryForMap("SELECT code, full_name FROM employee WHERE id = ?", target);
        List<TimesheetService.Day> days = timesheets.days(target, from, to);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("employeeId", target);
        out.put("employeeCode", e.get("code"));
        out.put("employeeName", e.get("full_name"));
        out.put("year", year);
        out.put("month", month);
        out.put("days", days);
        out.put("stdMinutes", days.stream().mapToInt(TimesheetService.Day::scheduledMinutes).sum());
        out.put("paidMinutes", days.stream().mapToInt(TimesheetService.Day::paidMinutes).sum());
        out.put("workedMinutes", days.stream().mapToInt(TimesheetService.Day::workedMinutes).sum());
        out.put("lateMinutes", days.stream().mapToInt(TimesheetService.Day::lateMinutes).sum());
        out.put("earlyMinutes", days.stream().mapToInt(TimesheetService.Day::earlyMinutes).sum());
        out.put("unpaidLeaveMinutes", days.stream().mapToInt(TimesheetService.Day::unpaidLeaveMinutes).sum());
        out.put("blockingIssues", timesheets.blockingCount(days));
        return out;
    }

    // ---------------- xác nhận / điều chỉnh ----------------

    /** Manager trực tiếp hoặc HR; không bao giờ tự xác nhận công của chính mình. */
    private void requireConfirmer(long employeeId, String action, long entityId) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        if (u.employeeId() != null && u.employeeId() == employeeId) {
            audit.denied("SELF_APPROVAL", "attendance", entityId, action + " công của chính mình");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không được tự xác nhận/duyệt công của mình");
        }
        if (!u.is("HR") && !employees.isTeamMember(u.employeeId(), employeeId)) {
            throw new ResourceNotFoundException("Không tìm thấy bản ghi chấm công");
        }
    }

    @Transactional
    public Map<String, Object> confirm(long id, String note) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        Map<String, Object> rec = Sql.one(jdbc, "Không tìm thấy bản ghi chấm công", SELECT + " WHERE a.id = ?", id);
        long emp = ((Number) rec.get("employeeId")).longValue();
        requireConfirmer(emp, "confirm", id);
        guard.assertOpen((LocalDate) rec.get("workDate"));
        if (!"NEEDS_REVIEW".equals(rec.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ xác nhận được bản ghi NEEDS_REVIEW");
        }
        if (rec.get("checkInAt") == null || rec.get("checkOutAt") == null) {
            throw BusinessException.unprocessable("CANNOT_CONFIRM_INCOMPLETE",
                    "Thiếu check-in/check-out: hãy tạo điều chỉnh công có minh chứng thay vì xác nhận");
        }
        jdbc.update("""
                UPDATE attendance_record SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = now(), note = COALESCE(?, note),
                       version = version + 1 WHERE id = ?
                """, u.userId(), note, id);
        audit.success("CONFIRM_ATTENDANCE", "attendance_record", id, note, Map.of("status", "NEEDS_REVIEW"), Map.of("status", "CONFIRMED"));
        return get(id);
    }

    public record AdjustmentCmd(Long employeeId, LocalDate workDate, Instant proposedCheckIn, Instant proposedCheckOut,
                                String method, String reason, String evidence) {
    }

    private static final String ADJ_SELECT = """
            SELECT x.id, x.attendance_id, x.employee_id, e.code AS employee_code, e.full_name AS employee_name, x.work_date,
                   x.proposed_check_in, x.proposed_check_out, x.method, x.reason, x.evidence, x.status,
                   COALESCE(re.full_name, ra.email) AS requested_by_name, COALESCE(de.full_name, da.email) AS decided_by_name,
                   x.decided_at, x.decision_note, x.created_at
            FROM attendance_adjustment x JOIN employee e ON e.id = x.employee_id
            JOIN user_account ra ON ra.id = x.requested_by LEFT JOIN employee re ON re.id = ra.employee_id
            LEFT JOIN user_account da ON da.id = x.decided_by LEFT JOIN employee de ON de.id = da.employee_id
            """;

    @Transactional
    public Map<String, Object> createAdjustment(AdjustmentCmd c) {
        AuthUser u = Principals.current();
        long mine = Principals.employeeId();
        long target = c.employeeId() == null ? mine : c.employeeId();
        if (target != mine && !u.is("HR")) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Chỉ HR được tạo điều chỉnh công thay nhân viên khác");
        }
        if (!METHODS.contains(c.method())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Phương thức công không hợp lệ");
        }
        if (c.workDate().isAfter(Vn.today())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Không điều chỉnh công cho ngày trong tương lai");
        }
        if (("MANUAL".equals(c.method()) || "QR".equals(c.method()))
                && (c.proposedCheckIn() == null || c.proposedCheckOut() == null || !c.proposedCheckOut().isAfter(c.proposedCheckIn()))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Phải có giờ vào/ra hợp lệ (giờ ra sau giờ vào)");
        }
        if ((c.proposedCheckIn() != null && !Vn.dateOf(c.proposedCheckIn()).equals(c.workDate()))
                || (c.proposedCheckOut() != null && !Vn.dateOf(c.proposedCheckOut()).equals(c.workDate()))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Giờ vào/ra phải thuộc ngày công");
        }
        guard.assertOpen(c.workDate());
        Long attendanceId = jdbc.query("SELECT id FROM attendance_record WHERE employee_id = ? AND work_date = ?",
                rs -> rs.next() ? rs.getLong(1) : null, target);
        Long id = jdbc.queryForObject("""
                INSERT INTO attendance_adjustment(attendance_id, employee_id, work_date, proposed_check_in, proposed_check_out,
                       method, reason, evidence, status, requested_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'PENDING', ?) RETURNING id
                """, Long.class, attendanceId, target, c.workDate(), c.proposedCheckIn() == null ? null : Timestamp.from(c.proposedCheckIn()),
                c.proposedCheckOut() == null ? null : Timestamp.from(c.proposedCheckOut()), c.method(), c.reason(), c.evidence(), u.userId());
        return Sql.one(jdbc, "Không tìm thấy yêu cầu", ADJ_SELECT + " WHERE x.id = ?", id);
    }

    public List<Map<String, Object>> adjustments(String status) {
        AuthUser u = Principals.current();
        requireAttendanceViewer(u);
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (u.is("EMPLOYEE")) {
            where.append(" AND x.employee_id = ?");
            args.add(u.employeeId());
        } else if (u.is("MANAGER")) {
            where.append(" AND (x.employee_id = ? OR e.manager_id = ?)");
            args.add(u.employeeId());
            args.add(u.employeeId());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND x.status = ?");
            args.add(status);
        }
        return Sql.list(jdbc, ADJ_SELECT + where + " ORDER BY x.id DESC", args.toArray());
    }

    @Transactional
    public Map<String, Object> decideAdjustment(long id, boolean approve, String note) {
        AuthUser u = Principals.requireRole("HR", "MANAGER");
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT * FROM attendance_adjustment WHERE id = ? FOR UPDATE", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy yêu cầu điều chỉnh công");
        }
        Map<String, Object> x = rows.get(0);
        long emp = ((Number) x.get("employee_id")).longValue();
        requireConfirmer(emp, approve ? "approve" : "reject", id);
        if (((Number) x.get("requested_by")).longValue() == u.userId()) {
            audit.denied("SELF_APPROVAL", "attendance_adjustment", id, "Người tạo tự duyệt điều chỉnh công");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không được tự duyệt yêu cầu do mình tạo");
        }
        if (!"PENDING".equals(x.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Yêu cầu đã được xử lý");
        }
        LocalDate date = ((java.sql.Date) x.get("work_date")).toLocalDate();
        guard.assertOpen(date);
        Map<String, Object> before = null;
        Map<String, Object> after = null;
        Long attendanceId = x.get("attendance_id") == null ? null : ((Number) x.get("attendance_id")).longValue();
        if (approve) {
            String method = (String) x.get("method");
            Timestamp in = (Timestamp) x.get("proposed_check_in");
            Timestamp out = (Timestamp) x.get("proposed_check_out");
            DayPlan plan = calendar.plan(emp, date);
            Result r = AttendanceCalc.compute(plan, in == null ? null : in.toInstant(), out == null ? null : out.toInstant(),
                    method, Vn.today());
            String status = "ABSENCE".equals(method) ? "ABSENT_CONFIRMED" : "CONFIRMED";
            List<Map<String, Object>> existing = jdbc.queryForList("SELECT * FROM attendance_record WHERE employee_id = ? AND work_date = ?", emp, date);
            if (existing.isEmpty()) {
                attendanceId = jdbc.queryForObject("""
                        INSERT INTO attendance_record(employee_id, work_date, check_in_at, check_out_at, method, scheduled_minutes,
                               worked_minutes, late_minutes, early_minutes, status, review_reason, confirmed_by, confirmed_at, note)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, now(), ?) RETURNING id
                        """, Long.class, emp, date, in, out, method, r.scheduledMinutes(), r.workedMinutes(), r.lateMinutes(),
                        r.earlyMinutes(), status, u.userId(), "Điều chỉnh #" + id);
            } else {
                before = Sql.camel(existing.get(0));
                attendanceId = ((Number) existing.get(0).get("id")).longValue();
                jdbc.update("""
                        UPDATE attendance_record SET check_in_at = ?, check_out_at = ?, method = ?, scheduled_minutes = ?, worked_minutes = ?,
                               late_minutes = ?, early_minutes = ?, status = ?, review_reason = NULL, confirmed_by = ?, confirmed_at = now(),
                               note = ?, version = version + 1 WHERE id = ?
                        """, in, out, method, r.scheduledMinutes(), r.workedMinutes(), r.lateMinutes(), r.earlyMinutes(), status,
                        u.userId(), "Điều chỉnh #" + id, attendanceId);
            }
            after = Map.of("method", method, "status", status, "workedMinutes", r.workedMinutes());
        }
        jdbc.update("""
                UPDATE attendance_adjustment SET status = ?, decided_by = ?, decided_at = now(), decision_note = ?, attendance_id = ?,
                       before_json = ?::jsonb, after_json = ?::jsonb WHERE id = ?
                """, approve ? "APPROVED" : "REJECTED", u.userId(), note, attendanceId,
                before == null ? null : json.write(before), after == null ? null : json.write(after), id);
        audit.success(approve ? "APPROVE_ATTENDANCE_ADJUSTMENT" : "REJECT_ATTENDANCE_ADJUSTMENT", "attendance_adjustment", id, note, before, after);
        return Sql.one(jdbc, "Không tìm thấy yêu cầu", ADJ_SELECT + " WHERE x.id = ?", id);
    }

    /** HR đánh dấu vắng không phép: ABSENT_CONFIRMED, không được trả (trừ phần nghỉ phép hưởng lương nếu có). */
    @Transactional
    public List<Map<String, Object>> markAbsences(long employeeId, List<LocalDate> dates, String reason) {
        AuthUser hr = Principals.requireRole("HR");
        if (hr.employeeId() != null && hr.employeeId() == employeeId) {
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không tự xác nhận vắng của chính mình");
        }
        employees.requireOperationalScope(employeeId);
        List<Map<String, Object>> out = new ArrayList<>();
        for (LocalDate d : dates) {
            guard.assertOpen(d);
            DayPlan plan = calendar.plan(employeeId, d);
            Integer existing = jdbc.queryForObject("SELECT COUNT(*) FROM attendance_record WHERE employee_id = ? AND work_date = ? AND check_in_at IS NOT NULL AND status IN ('VALID','CONFIRMED')",
                    Integer.class, employeeId, d);
            if (existing != null && existing > 0) {
                throw BusinessException.conflict("INVALID_STATE", "Ngày " + d + " đã có chấm công hợp lệ, hãy dùng điều chỉnh công");
            }
            jdbc.update("DELETE FROM attendance_record WHERE employee_id = ? AND work_date = ? AND status IN ('NEEDS_REVIEW','ABSENT_CONFIRMED')", employeeId, d);
            Long id = jdbc.queryForObject("""
                    INSERT INTO attendance_record(employee_id, work_date, method, scheduled_minutes, status, confirmed_by, confirmed_at, note)
                    VALUES (?, ?, 'ABSENCE', ?, 'ABSENT_CONFIRMED', ?, now(), ?) RETURNING id
                    """, Long.class, employeeId, d, plan.scheduledMinutes(), hr.userId(), reason);
            out.add(Sql.one(jdbc, "Không tìm thấy bản ghi", SELECT + " WHERE a.id = ?", id));
        }
        audit.success("MARK_ABSENCE", "attendance_record", employeeId, reason, null, Map.of("dates", dates.toString()));
        return out;
    }

    // ---------------- điểm chấm công, QR, ảnh ----------------

    public List<Map<String, Object>> points() {
        Principals.current();
        return Sql.list(jdbc, "SELECT id, code, name, photo_required, active FROM attendance_point ORDER BY id");
    }

    @Transactional
    public Map<String, Object> savePoint(Long id, String code, String name, Boolean photoRequired, Boolean active) {
        Principals.requireRole("HR");
        boolean photo = photoRequired == null || photoRequired;
        boolean act = active == null || active;
        if (id == null) {
            Long nid = jdbc.queryForObject("INSERT INTO attendance_point(code, name, photo_required, active) VALUES (?, ?, ?, ?) RETURNING id",
                    Long.class, code.trim(), name.trim(), photo, act);
            audit.success("CREATE_ATTENDANCE_POINT", "attendance_point", nid, name, null, null);
            return Sql.one(jdbc, "Không tìm thấy điểm", "SELECT id, code, name, photo_required, active FROM attendance_point WHERE id = ?", nid);
        }
        int n = jdbc.update("UPDATE attendance_point SET code = ?, name = ?, photo_required = ?, active = ? WHERE id = ?", code.trim(), name.trim(), photo, act, id);
        if (n == 0) {
            throw new ResourceNotFoundException("Không tìm thấy điểm chấm công");
        }
        audit.success("UPDATE_ATTENDANCE_POINT", "attendance_point", id, name, null, null);
        return Sql.one(jdbc, "Không tìm thấy điểm", "SELECT id, code, name, photo_required, active FROM attendance_point WHERE id = ?", id);
    }

    @Transactional
    public Map<String, Object> issueQr(long pointId) {
        AuthUser hr = Principals.requireRole("HR");
        Map<String, Object> p = Sql.one(jdbc, "Không tìm thấy điểm chấm công", "SELECT id, name, photo_required, active FROM attendance_point WHERE id = ?", pointId);
        if (!Boolean.TRUE.equals(p.get("active"))) {
            throw BusinessException.conflict("INVALID_STATE", "Điểm chấm công đã ngừng hoạt động");
        }
        SecureRandom rnd = new SecureRandom();
        byte[] buf = new byte[16];
        rnd.nextBytes(buf);
        StringBuilder nonce = new StringBuilder();
        for (byte b : buf) {
            nonce.append(String.format("%02x", b));
        }
        String shortCode = String.format("%06d", rnd.nextInt(1_000_000));
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(qrTtl);
        jdbc.update("INSERT INTO qr_token(point_id, nonce, short_code, issued_at, expires_at, issued_by) VALUES (?, ?, ?, ?, ?, ?)",
                pointId, nonce.toString(), shortCode, Timestamp.from(now), Timestamp.from(exp), hr.userId());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pointId", pointId);
        out.put("pointName", p.get("name"));
        out.put("qrPayload", QR_PREFIX + nonce);
        out.put("shortCode", shortCode);
        out.put("issuedAt", now);
        out.put("expiresAt", exp);
        out.put("ttlSeconds", qrTtl);
        out.put("photoRequired", p.get("photoRequired"));
        return out;
    }

    public Map<String, Object> photo(long id, String kind) {
        Map<String, Object> rec = Sql.one(jdbc, "Không tìm thấy bản ghi chấm công",
                "SELECT employee_id, check_in_photo_id, check_out_photo_id FROM attendance_record WHERE id = ?", id);
        employees.requireOperationalScope(((Number) rec.get("employeeId")).longValue());
        Object photoId = "out".equalsIgnoreCase(kind) ? rec.get("checkOutPhotoId") : rec.get("checkInPhotoId");
        if (photoId == null) {
            throw new ResourceNotFoundException("Không có ảnh minh chứng");
        }
        Map<String, Object> p = jdbc.queryForMap("SELECT content, mime_type FROM attendance_photo WHERE id = ?", photoId);
        audit.success("VIEW_ATTENDANCE_PHOTO", "attendance_record", id, kind, null, null);
        return p;
    }
}
