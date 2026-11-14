package com.hrflow.auth;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.util.Sql;
import com.hrflow.common.util.Vn;
import com.hrflow.security.AuthUser;
import com.hrflow.security.JwtService;
import com.hrflow.security.PayrollAccess;
import com.hrflow.security.Principals;

@Service
public class AuthService {

    static final int MAX_FAILED = 5;
    static final int LOCK_MINUTES = 15;
    private static final int PASSWORD_HISTORY = 3;

    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final PayrollAccess payrollAccess;
    private final AuditService audit;

    public AuthService(JdbcTemplate jdbc, PasswordEncoder encoder, JwtService jwt, PayrollAccess payrollAccess,
                       AuditService audit) {
        this.jdbc = jdbc;
        this.encoder = encoder;
        this.jwt = jwt;
        this.payrollAccess = payrollAccess;
        this.audit = audit;
    }

    /** Không dùng @Transactional: bộ đếm đăng nhập sai phải được ghi dù request trả 401. */
    public Map<String, Object> login(String email, String password) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT a.id, a.email, a.password_hash, a.status, a.token_version, a.locked_until, a.disable_at,
                       a.failed_attempts, e.status AS emp_status, e.termination_date
                FROM user_account a LEFT JOIN employee e ON e.id = a.employee_id
                WHERE lower(a.email) = lower(?)
                """, email);
        if (rows.isEmpty()) {
            throw unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
        }
        Map<String, Object> a = rows.get(0);
        long id = ((Number) a.get("id")).longValue();
        Timestamp lockedUntil = (Timestamp) a.get("locked_until");
        if (lockedUntil != null && lockedUntil.toInstant().isAfter(java.time.Instant.now())) {
            throw unauthorized("ACCOUNT_TEMP_LOCKED", "Tài khoản bị khóa tạm thời do đăng nhập sai nhiều lần");
        }
        if (!encoder.matches(password, (String) a.get("password_hash"))) {
            int failed = ((Number) a.get("failed_attempts")).intValue() + 1;
            if (failed >= MAX_FAILED) {
                jdbc.update("UPDATE user_account SET failed_attempts = 0, locked_until = now() + (? * interval '1 minute') WHERE id = ?",
                        LOCK_MINUTES, id);
                audit.record("LOGIN_LOCKED", "user_account", id, "DENIED", "Sai mật khẩu " + MAX_FAILED + " lần", null, null);
                throw unauthorized("ACCOUNT_TEMP_LOCKED", "Tài khoản bị khóa tạm thời do đăng nhập sai nhiều lần");
            }
            jdbc.update("UPDATE user_account SET failed_attempts = ? WHERE id = ?", failed, id);
            throw unauthorized("INVALID_CREDENTIALS", "Email hoặc mật khẩu không đúng");
        }
        String status = (String) a.get("status");
        if (!"ACTIVE".equals(status)) {
            throw unauthorized("ACCOUNT_UNAVAILABLE", "Tài khoản bị khóa hoặc đã ngừng hoạt động");
        }
        java.sql.Date disableAt = (java.sql.Date) a.get("disable_at");
        java.sql.Date termination = (java.sql.Date) a.get("termination_date");
        LocalDate today = Vn.today();
        boolean terminated = "TERMINATED".equals(a.get("emp_status")) && termination != null
                && termination.toLocalDate().isBefore(today);
        if ((disableAt != null && disableAt.toLocalDate().isBefore(today)) || terminated) {
            throw unauthorized("ACCOUNT_UNAVAILABLE", "Tài khoản đã hết hiệu lực do nhân viên nghỉ việc");
        }
        jdbc.update("UPDATE user_account SET failed_attempts = 0, locked_until = NULL, last_login_at = now() WHERE id = ?", id);
        return tokenResponse(id);
    }

    public Map<String, Object> tokenResponse(long accountId) {
        Map<String, Object> a = jdbc.queryForMap("SELECT email, token_version, must_change_password FROM user_account WHERE id = ?",
                accountId);
        JwtService.Issued issued = jwt.generate(accountId, (String) a.get("email"),
                ((Number) a.get("token_version")).intValue());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", issued.token());
        out.put("expiresAt", issued.expiresAt());
        out.put("mustChangePassword", a.get("must_change_password"));
        out.put("user", me(accountId));
        return out;
    }

    public Map<String, Object> me(long accountId) {
        Map<String, Object> row = Sql.camel(jdbc.queryForMap("""
                SELECT a.id AS account_id, a.email, a.role, a.employee_id, a.must_change_password,
                       e.full_name, e.code AS employee_code, d.name AS department_name,
                       j.code AS job_title_code, j.name AS job_title_name
                FROM user_account a
                LEFT JOIN employee e ON e.id = a.employee_id
                LEFT JOIN department d ON d.id = e.department_id
                LEFT JOIN job_title j ON j.id = e.job_title_id
                WHERE a.id = ?
                """, accountId));
        Long empId = row.get("employeeId") == null ? null : ((Number) row.get("employeeId")).longValue();
        row.put("grants", payrollAccess.activePermissions(accountId));
        row.put("teamSize", empId == null ? 0 : jdbc.queryForObject(
                "SELECT COUNT(*) FROM employee WHERE manager_id = ? AND status <> 'TERMINATED'", Integer.class, empId));
        row.put("approver", empId != null && Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS (SELECT 1 FROM approval_route WHERE (primary_approver_id = ? OR backup_approver_id = ?)
                               AND (effective_to IS NULL OR effective_to >= current_date))
                """, Boolean.class, empId, empId)));
        return row;
    }

    @Transactional
    public Map<String, Object> changePassword(String current, String next) {
        AuthUser u = Principals.current();
        Map<String, Object> a = jdbc.queryForMap("SELECT password_hash FROM user_account WHERE id = ?", u.userId());
        if (!encoder.matches(current, (String) a.get("password_hash"))) {
            throw BusinessException.unprocessable("CURRENT_PASSWORD_INVALID", "Mật khẩu hiện tại không đúng");
        }
        validatePolicy(next);
        if (encoder.matches(next, (String) a.get("password_hash"))) {
            throw BusinessException.unprocessable("PASSWORD_REUSED", "Mật khẩu mới phải khác mật khẩu cũ");
        }
        List<String> history = jdbc.queryForList("""
                SELECT password_hash FROM password_history WHERE account_id = ? ORDER BY id DESC LIMIT ?
                """, String.class, u.userId(), PASSWORD_HISTORY);
        for (String h : history) {
            if (encoder.matches(next, h)) {
                throw BusinessException.unprocessable("PASSWORD_REUSED", "Không dùng lại các mật khẩu gần đây");
            }
        }
        setPassword(u.userId(), next, false);
        audit.success("CHANGE_PASSWORD", "user_account", u.userId(), null, null, null);
        return tokenResponse(u.userId());
    }

    /** Đặt mật khẩu, lưu lịch sử và tăng token_version để thu hồi token cũ. */
    public void setPassword(long accountId, String rawPassword, boolean mustChange) {
        String hash = encoder.encode(rawPassword);
        jdbc.update("""
                UPDATE user_account SET password_hash = ?, must_change_password = ?, token_version = token_version + 1,
                       failed_attempts = 0, locked_until = NULL WHERE id = ?
                """, hash, mustChange, accountId);
        jdbc.update("INSERT INTO password_history(account_id, password_hash) VALUES (?, ?)", accountId, hash);
    }

    public void logout() {
        jdbc.update("UPDATE user_account SET token_version = token_version + 1 WHERE id = ?", Principals.current().userId());
    }

    public Map<String, Object> refresh() {
        return tokenResponse(Principals.current().userId());
    }

    public static void validatePolicy(String p) {
        boolean ok = p != null && p.length() >= 10 && p.chars().anyMatch(Character::isUpperCase)
                && p.chars().anyMatch(Character::isLowerCase) && p.chars().anyMatch(Character::isDigit)
                && p.chars().anyMatch(c -> !Character.isLetterOrDigit(c));
        if (!ok) {
            throw BusinessException.unprocessable("PASSWORD_POLICY",
                    "Mật khẩu tối thiểu 10 ký tự, gồm chữ hoa, chữ thường, số và ký tự đặc biệt");
        }
    }

    private static BusinessException unauthorized(String code, String message) {
        return new BusinessException(org.springframework.http.HttpStatus.UNAUTHORIZED, code, message);
    }
}
