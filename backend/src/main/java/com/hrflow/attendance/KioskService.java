package com.hrflow.attendance;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.attendance.AttendanceCalc.Result;
import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.util.Vn;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/**
 * Máy chấm công tại quầy (SRS FR-07, DEC-10). Chỉ tài khoản thiết bị KIOSK gọi được (SecurityConfig).
 * <ol>
 *   <li>{@link #scan}: xác thực QR cá nhân, xác định CHECK_IN/CHECK_OUT, giữ mã cho máy quầy – chưa ghi công;</li>
 *   <li>{@link #punch}: nhận ảnh chụp tại quầy, ghi check-in/check-out với giờ server lúc quét, đánh dấu mã đã dùng.</li>
 * </ol>
 * Nhân viên của bản ghi lấy từ mã QR do server cấp; điểm chấm công lấy từ tài khoản KIOSK – không tin dữ liệu client.
 */
@Service
public class KioskService {

    static final String CHECK_IN = "CHECK_IN";
    static final String CHECK_OUT = "CHECK_OUT";
    private static final Set<String> MIMES = Set.of("image/jpeg", "image/png");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private record Kiosk(long accountId, String email, long pointId, String pointCode, String pointName) {
    }

    private final JdbcTemplate jdbc;
    private final WorkCalendar calendar;
    private final PeriodGuard guard;
    private final AuditService audit;
    private final int punchWindowSeconds;
    private final int minCheckoutGapMinutes;
    private final int photoMaxKb;

    public KioskService(JdbcTemplate jdbc, WorkCalendar calendar, PeriodGuard guard, AuditService audit,
                        @Value("${hrflow.attendance.punch-window-seconds:30}") int punchWindowSeconds,
                        @Value("${hrflow.attendance.min-checkout-gap-minutes:30}") int minCheckoutGapMinutes,
                        @Value("${hrflow.attendance.photo-max-kb:200}") int photoMaxKb) {
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.guard = guard;
        this.audit = audit;
        this.punchWindowSeconds = punchWindowSeconds;
        this.minCheckoutGapMinutes = minCheckoutGapMinutes;
        this.photoMaxKb = photoMaxKb;
    }

    private Kiosk currentKiosk() {
        AuthUser u = Principals.requireRole("KIOSK");
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT p.id, p.code, p.name, p.active FROM user_account a JOIN attendance_point p ON p.id = a.attendance_point_id
                WHERE a.id = ?
                """, u.userId());
        if (rows.isEmpty()) {
            throw BusinessException.conflict("INVALID_STATE", "Máy chấm công chưa được gắn điểm chấm công");
        }
        Map<String, Object> p = rows.get(0);
        if (!Boolean.TRUE.equals(p.get("active"))) {
            throw reject(null, BusinessException.conflict("INVALID_STATE", "Điểm chấm công đã ngừng hoạt động"));
        }
        return new Kiosk(u.userId(), u.email(), ((Number) p.get("id")).longValue(), (String) p.get("code"),
                (String) p.get("name"));
    }

    /** Ghi audit lượt bị từ chối (giao dịch riêng, không mất khi rollback) rồi trả lại lỗi để ném. */
    private BusinessException reject(Object tokenId, BusinessException ex) {
        audit.denied("KIOSK_SCAN_REJECTED", "employee_qr_token", tokenId, ex.getCode() + ": " + ex.getMessage());
        return ex;
    }

    public Map<String, Object> me() {
        Kiosk k = currentKiosk();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("accountId", k.accountId());
        out.put("email", k.email());
        out.put("pointId", k.pointId());
        out.put("pointCode", k.pointCode());
        out.put("pointName", k.pointName());
        out.put("photoMaxKb", photoMaxKb);
        return out;
    }

    // ---------------- Bước 1: quét QR ----------------

    @Transactional
    public Map<String, Object> scan(String qrPayload) {
        Kiosk k = currentKiosk();
        String payload = qrPayload == null ? "" : qrPayload.trim();
        if (!payload.startsWith(EmployeeQrService.PREFIX)) {
            throw reject(null, BusinessException.unprocessable("QR_INVALID", "Mã QR không hợp lệ"));
        }
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT t.id, t.employee_id, t.expires_at, t.revoked_at, t.scanned_at, t.used_at,
                       e.code, e.full_name, e.status
                FROM employee_qr_token t JOIN employee e ON e.id = t.employee_id
                WHERE t.nonce = ? FOR UPDATE OF t
                """, payload.substring(EmployeeQrService.PREFIX.length()));
        if (rows.isEmpty()) {
            throw reject(null, BusinessException.unprocessable("QR_INVALID", "Mã QR không hợp lệ"));
        }
        Map<String, Object> t = rows.get(0);
        long tokenId = ((Number) t.get("id")).longValue();
        long emp = ((Number) t.get("employee_id")).longValue();
        Instant now = Instant.now();
        if (t.get("used_at") != null || t.get("scanned_at") != null) {
            throw reject(tokenId, BusinessException.unprocessable("QR_USED", "Mã QR đã được sử dụng, hãy mở mã mới trên điện thoại"));
        }
        if (t.get("revoked_at") != null || ((Timestamp) t.get("expires_at")).toInstant().isBefore(now)) {
            throw reject(tokenId, BusinessException.unprocessable("QR_EXPIRED", "Mã QR đã hết hạn, hãy dùng mã mới trên điện thoại"));
        }
        String status = (String) t.get("status");
        if ("TERMINATED".equals(status) || "ONBOARDING".equals(status)) {
            throw reject(tokenId, BusinessException.conflict("INVALID_STATE", "Nhân viên chưa/không còn làm việc nên không chấm công"));
        }
        LocalDate today = Vn.dateOf(now);
        try {
            guard.assertOpen(today);
        } catch (BusinessException ex) {
            throw reject(tokenId, ex);
        }
        String action = decideAction(tokenId, emp, today, now);
        Instant deadline = now.plusSeconds(punchWindowSeconds);
        jdbc.update("""
                UPDATE employee_qr_token SET scanned_at = ?, scanned_by = ?, punch_type = ?, punch_deadline = ? WHERE id = ?
                """, Timestamp.from(now), k.accountId(), action, Timestamp.from(deadline), tokenId);
        audit.success("KIOSK_SCAN", "employee_qr_token", tokenId, k.pointName(), null,
                Map.of("employeeId", emp, "action", action));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scanId", tokenId);
        out.put("employeeCode", t.get("code"));
        out.put("employeeName", t.get("full_name"));
        out.put("action", action);
        out.put("scannedAt", now);
        out.put("punchDeadline", deadline);
        out.put("photoMaxKb", photoMaxKb);
        return out;
    }

    /** Chưa có bản ghi hôm nay -> CHECK_IN; đã check-in đủ lâu, chưa check-out -> CHECK_OUT; còn lại từ chối. */
    private String decideAction(long tokenId, long emp, LocalDate day, Instant now) {
        List<Map<String, Object>> recs = jdbc.queryForList(
                "SELECT check_in_at, check_out_at, method FROM attendance_record WHERE employee_id = ? AND work_date = ?", emp, day);
        if (recs.isEmpty()) {
            return CHECK_IN;
        }
        Map<String, Object> r = recs.get(0);
        Timestamp in = (Timestamp) r.get("check_in_at");
        if (in == null || !("QR".equals(r.get("method")) || "MANUAL".equals(r.get("method")))) {
            throw reject(tokenId, BusinessException.conflict("INVALID_STATE",
                    "Hôm nay đã có bản ghi công khác (vắng/điều chỉnh); liên hệ HR"));
        }
        if (r.get("check_out_at") != null) {
            throw reject(tokenId, BusinessException.conflict("ALREADY_CHECKED_OUT", "Hôm nay bạn đã check-in và check-out"));
        }
        if (Duration.between(in.toInstant(), now).toMinutes() < minCheckoutGapMinutes) {
            throw reject(tokenId, BusinessException.conflict("TOO_SOON_TO_CHECK_OUT",
                    "Bạn đã check-in lúc " + HHMM.format(Vn.at(in.toInstant()))));
        }
        return CHECK_OUT;
    }

    // ---------------- Bước 2: chụp ảnh và ghi công ----------------

    @Transactional
    public Map<String, Object> punch(long scanId, String photoBase64, String photoMime) {
        Kiosk k = currentKiosk();
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT t.id, t.employee_id, t.nonce, t.scanned_at, t.scanned_by, t.punch_type, t.punch_deadline, t.used_at,
                       e.code, e.full_name
                FROM employee_qr_token t JOIN employee e ON e.id = t.employee_id
                WHERE t.id = ? FOR UPDATE OF t
                """, scanId);
        if (rows.isEmpty() || rows.get(0).get("scanned_at") == null) {
            throw reject(scanId, BusinessException.unprocessable("SCAN_EXPIRED", "Phiên quét không tồn tại, hãy quét lại"));
        }
        Map<String, Object> t = rows.get(0);
        if (((Number) t.get("scanned_by")).longValue() != k.accountId()) {
            throw reject(scanId, BusinessException.forbidden("FORBIDDEN", "Phiên quét thuộc máy chấm công khác"));
        }
        if (t.get("used_at") != null) {
            throw reject(scanId, BusinessException.unprocessable("QR_USED", "Mã QR đã được sử dụng"));
        }
        if (((Timestamp) t.get("punch_deadline")).toInstant().isBefore(Instant.now())) {
            throw reject(scanId, BusinessException.unprocessable("SCAN_EXPIRED", "Quá thời gian gửi ảnh, hãy quét lại bằng mã mới"));
        }
        byte[] photo = decodePhoto(scanId, photoBase64, photoMime);
        String mime = photoMime == null || photoMime.isBlank() ? "image/jpeg" : photoMime;

        long emp = ((Number) t.get("employee_id")).longValue();
        Instant at = ((Timestamp) t.get("scanned_at")).toInstant();
        LocalDate day = Vn.dateOf(at);
        guard.assertOpen(day);
        String action = (String) t.get("punch_type");
        String nonce = (String) t.get("nonce");

        long photoId = insertPhoto(emp, photo, mime, k.accountId());
        long attendanceId;
        Result r;
        List<Map<String, Object>> recs = jdbc.queryForList("""
                SELECT id, check_in_at, check_out_at, method FROM attendance_record WHERE employee_id = ? AND work_date = ? FOR UPDATE
                """, emp, day);
        if (CHECK_IN.equals(action)) {
            if (!recs.isEmpty()) {
                throw BusinessException.conflict("INVALID_STATE", "Dữ liệu công đã thay đổi kể từ lúc quét, hãy quét lại");
            }
            r = AttendanceCalc.compute(calendar.plan(emp, day), at, null, "QR", Vn.today());
            attendanceId = jdbc.queryForObject("""
                    INSERT INTO attendance_record(employee_id, work_date, check_in_at, check_in_point_id, check_in_kiosk_id,
                           check_in_nonce, check_in_photo_id, method, scheduled_minutes, worked_minutes, late_minutes,
                           early_minutes, status, review_reason)
                    VALUES (?, ?, ?, ?, ?, ?, ?, 'QR', ?, ?, ?, ?, ?, ?) RETURNING id
                    """, Long.class, emp, day, Timestamp.from(at), k.pointId(), k.accountId(), nonce, photoId,
                    r.scheduledMinutes(), r.workedMinutes(), r.lateMinutes(), r.earlyMinutes(), r.status(), r.reviewReason());
        } else {
            if (recs.isEmpty() || recs.get(0).get("check_in_at") == null || recs.get(0).get("check_out_at") != null) {
                throw BusinessException.conflict("INVALID_STATE", "Dữ liệu công đã thay đổi kể từ lúc quét, hãy quét lại");
            }
            Map<String, Object> rec = recs.get(0);
            attendanceId = ((Number) rec.get("id")).longValue();
            Instant in = ((Timestamp) rec.get("check_in_at")).toInstant();
            r = AttendanceCalc.compute(calendar.plan(emp, day), in, at, (String) rec.get("method"), Vn.today());
            jdbc.update("""
                    UPDATE attendance_record SET check_out_at = ?, check_out_point_id = ?, check_out_kiosk_id = ?,
                           check_out_nonce = ?, check_out_photo_id = ?, scheduled_minutes = ?, worked_minutes = ?,
                           late_minutes = ?, early_minutes = ?, status = ?, review_reason = ?, version = version + 1
                    WHERE id = ?
                    """, Timestamp.from(at), k.pointId(), k.accountId(), nonce, photoId, r.scheduledMinutes(),
                    r.workedMinutes(), r.lateMinutes(), r.earlyMinutes(), r.status(), r.reviewReason(), attendanceId);
        }
        jdbc.update("UPDATE employee_qr_token SET used_at = now(), attendance_id = ? WHERE id = ?", attendanceId, scanId);
        audit.success("KIOSK_PUNCH", "attendance_record", attendanceId, k.pointName(), null,
                Map.of("employeeId", emp, "action", action, "at", at.toString()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("action", action);
        out.put("employeeCode", t.get("code"));
        out.put("employeeName", t.get("full_name"));
        out.put("punchedAt", at);
        out.put("pointName", k.pointName());
        out.put("attendanceStatus", r.status());
        out.put("lateMinutes", r.lateMinutes());
        out.put("earlyMinutes", r.earlyMinutes());
        return out;
    }

    private byte[] decodePhoto(long scanId, String photoBase64, String photoMime) {
        if (photoBase64 == null || photoBase64.isBlank()) {
            throw reject(scanId, BusinessException.unprocessable("PHOTO_REQUIRED", "Thiếu ảnh minh chứng"));
        }
        if (photoMime != null && !photoMime.isBlank() && !MIMES.contains(photoMime)) {
            throw reject(scanId, BusinessException.unprocessable("PHOTO_INVALID", "Ảnh minh chứng phải là JPEG hoặc PNG"));
        }
        String data = photoBase64.trim();
        int comma = data.indexOf(',');
        if (data.startsWith("data:") && comma > 0) {
            data = data.substring(comma + 1);
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException ex) {
            throw reject(scanId, BusinessException.unprocessable("PHOTO_INVALID", "Ảnh minh chứng không hợp lệ"));
        }
        if (bytes.length == 0) {
            throw reject(scanId, BusinessException.unprocessable("PHOTO_REQUIRED", "Thiếu ảnh minh chứng"));
        }
        if (bytes.length > photoMaxKb * 1024L) {
            throw reject(scanId, BusinessException.unprocessable("PHOTO_TOO_LARGE", "Ảnh minh chứng tối đa " + photoMaxKb + " KB"));
        }
        return bytes;
    }

    private long insertPhoto(long employeeId, byte[] content, String mime, long kioskId) {
        KeyHolder kh = new GeneratedKeyHolder();
        jdbc.update(con -> {
            var ps = con.prepareStatement(
                    "INSERT INTO attendance_photo(employee_id, content, mime_type, size_bytes, captured_by) VALUES (?, ?, ?, ?, ?)",
                    new String[] {"id"});
            ps.setLong(1, employeeId);
            ps.setBytes(2, content);
            ps.setString(3, mime);
            ps.setInt(4, content.length);
            ps.setLong(5, kioskId);
            return ps;
        }, kh);
        return kh.getKey().longValue();
    }
}
