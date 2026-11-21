package com.hrflow.attendance;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.security.Principals;

/**
 * QR cá nhân động của nhân viên (SRS FR-07, DEC-10): server cấp cho đúng nhân viên đang đăng nhập,
 * nội dung chỉ gồm tiền tố + mã ngẫu nhiên, hạn ngắn (mặc định 15 giây), dùng một lần.
 * Máy chấm công tại quầy quét mã này (KioskService).
 */
@Service
public class EmployeeQrService {

    static final String PREFIX = "HRFLOW-EMP:";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final JdbcTemplate jdbc;
    private final int ttlSeconds;

    public EmployeeQrService(JdbcTemplate jdbc,
                             @Value("${hrflow.attendance.employee-qr-ttl-seconds:15}") int ttlSeconds) {
        this.jdbc = jdbc;
        this.ttlSeconds = ttlSeconds;
    }

    @Transactional
    public Map<String, Object> issue() {
        // employee_id lấy từ phiên; Admin/KIOSK không có hồ sơ nhân viên -> 403
        long emp = Principals.employeeId();
        Map<String, Object> e = jdbc.queryForMap("SELECT code, full_name, status FROM employee WHERE id = ?", emp);
        String status = (String) e.get("status");
        if ("TERMINATED".equals(status) || "ONBOARDING".equals(status)) {
            throw BusinessException.conflict("INVALID_STATE", "Nhân viên chưa/không còn làm việc nên không chấm công");
        }
        // Mỗi nhân viên một mã còn hiệu lực; không hủy mã đang được máy quầy giữ (đang chụp ảnh).
        jdbc.update("""
                UPDATE employee_qr_token SET revoked_at = now()
                WHERE employee_id = ? AND used_at IS NULL AND revoked_at IS NULL AND scanned_at IS NULL
                """, emp);
        byte[] buf = new byte[24];
        RANDOM.nextBytes(buf);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(buf);
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        jdbc.update("INSERT INTO employee_qr_token(employee_id, nonce, issued_at, expires_at) VALUES (?, ?, ?, ?)",
                emp, nonce, Timestamp.from(now), Timestamp.from(exp));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("qrPayload", PREFIX + nonce);
        out.put("employeeCode", e.get("code"));
        out.put("employeeName", e.get("full_name"));
        out.put("issuedAt", now);
        out.put("expiresAt", exp);
        out.put("ttlSeconds", ttlSeconds);
        return out;
    }
}
