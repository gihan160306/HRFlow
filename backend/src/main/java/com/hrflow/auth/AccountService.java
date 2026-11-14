package com.hrflow.auth;

import java.security.SecureRandom;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.util.Sql;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/** Tài khoản, đề nghị cấp tài khoản, grant payroll, tuyến duyệt, khai báo vắng mặt (nhóm A). */
@Service
public class AccountService {

    private static final Set<String> ROLES = Set.of("EMPLOYEE", "MANAGER", "HR", "ADMIN");
    private static final Set<String> PERMS = Set.of("VIEW", "PREPARE", "APPROVE", "LOCK");
    private static final String ACCOUNT_SELECT = """
            SELECT a.id, a.email, a.role, a.status, a.employee_id, e.code AS employee_code, e.full_name AS employee_name,
                   a.must_change_password, a.locked_until, a.disable_at, a.last_login_at
            FROM user_account a LEFT JOIN employee e ON e.id = a.employee_id
            """;

    private final JdbcTemplate jdbc;
    private final AuthService auth;
    private final AuditService audit;

    public AccountService(JdbcTemplate jdbc, AuthService auth, AuditService audit) {
        this.jdbc = jdbc;
        this.auth = auth;
        this.audit = audit;
    }

    // ---------------- Tài khoản (ADMIN) ----------------

    public List<Map<String, Object>> listAccounts() {
        Principals.requireRole("ADMIN");
        return Sql.camel(jdbc.queryForList(ACCOUNT_SELECT + " ORDER BY a.id"));
    }

    public Map<String, Object> getAccount(long id) {
        List<Map<String, Object>> rows = jdbc.queryForList(ACCOUNT_SELECT + " WHERE a.id = ?", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản");
        }
        return Sql.camel(rows.get(0));
    }

    @Transactional
    public Map<String, Object> createAccount(Long employeeId, String email, String role, String tempPassword,
                                             String reason, Long requestId) {
        AuthUser admin = Principals.requireRole("ADMIN");
        requireRole(role);
        if (employeeId == null && !"ADMIN".equals(role)) {
            throw BusinessException.unprocessable("EMPLOYEE_REQUIRED", "Tài khoản không phải Admin phải gắn hồ sơ nhân viên");
        }
        if (employeeId != null) {
            if ("ADMIN".equals(role)) {
                throw BusinessException.unprocessable("ADMIN_NOT_EMPLOYEE", "Admin kỹ thuật không gắn hồ sơ nhân viên");
            }
            Integer emp = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = ? AND status <> 'TERMINATED'",
                    Integer.class, employeeId);
            if (emp == null || emp == 0) {
                throw new ResourceNotFoundException("Không tìm thấy nhân viên đang làm việc");
            }
            Integer has = jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE employee_id = ?", Integer.class, employeeId);
            if (has != null && has > 0) {
                throw BusinessException.conflict("ACCOUNT_EXISTS", "Nhân viên đã có tài khoản");
            }
        }
        Integer dup = jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE lower(email) = lower(?)", Integer.class, email);
        if (dup != null && dup > 0) {
            throw BusinessException.conflict("DUPLICATE_RESOURCE", "Email đã được sử dụng");
        }
        String password = tempPassword == null || tempPassword.isBlank() ? generatePassword() : tempPassword;
        AuthService.validatePolicy(password);
        Long id = jdbc.queryForObject("""
                INSERT INTO user_account(email, password_hash, role, status, employee_id, must_change_password)
                VALUES (?, 'x', ?, 'ACTIVE', ?, TRUE) RETURNING id
                """, Long.class, email.trim(), role, employeeId);
        auth.setPassword(id, password, true);
        if (requestId != null) {
            jdbc.update("""
                    UPDATE account_request SET status = 'DONE', processed_by = ?, processed_at = now(), created_account_id = ?
                    WHERE id = ? AND status = 'PENDING'
                    """, admin.userId(), id, requestId);
        }
        audit.success("CREATE_ACCOUNT", "user_account", id, reason, null, Map.of("email", email, "role", role));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("account", getAccount(id));
        out.put("temporaryPassword", password);
        return out;
    }

    @Transactional
    public Map<String, Object> lock(long id, String reason) {
        Principals.requireRole("ADMIN");
        Map<String, Object> acc = getAccount(id);
        if (((Number) Principals.current().userId()).longValue() == id) {
            throw BusinessException.unprocessable("SELF_LOCK", "Không tự khóa tài khoản của mình");
        }
        jdbc.update("UPDATE user_account SET status = 'LOCKED', token_version = token_version + 1 WHERE id = ? AND status <> 'DISABLED'", id);
        audit.success("LOCK_ACCOUNT", "user_account", id, reason, Map.of("status", acc.get("status")), Map.of("status", "LOCKED"));
        return getAccount(id);
    }

    @Transactional
    public Map<String, Object> unlock(long id, String reason) {
        Principals.requireRole("ADMIN");
        getAccount(id);
        jdbc.update("UPDATE user_account SET status = 'ACTIVE', failed_attempts = 0, locked_until = NULL WHERE id = ? AND status = 'LOCKED'", id);
        audit.success("UNLOCK_ACCOUNT", "user_account", id, reason, null, null);
        return getAccount(id);
    }

    @Transactional
    public Map<String, Object> resetPassword(long id, String reason) {
        Principals.requireRole("ADMIN");
        getAccount(id);
        String password = generatePassword();
        auth.setPassword(id, password, true);
        audit.success("RESET_PASSWORD", "user_account", id, reason, null, null);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("account", getAccount(id));
        out.put("temporaryPassword", password);
        return out;
    }

    @Transactional
    public Map<String, Object> changeRole(long id, String role, String reason) {
        AuthUser admin = Principals.requireRole("ADMIN");
        requireRole(role);
        Map<String, Object> acc = getAccount(id);
        if (admin.userId() == id) {
            throw BusinessException.forbidden("SELF_ROLE_CHANGE", "Không tự đổi role của chính mình");
        }
        boolean hasEmployee = acc.get("employeeId") != null;
        if (hasEmployee && "ADMIN".equals(role)) {
            throw BusinessException.unprocessable("ADMIN_NOT_EMPLOYEE", "Tài khoản gắn hồ sơ nhân viên không thể là Admin");
        }
        if (!hasEmployee && !"ADMIN".equals(role)) {
            throw BusinessException.unprocessable("EMPLOYEE_REQUIRED", "Tài khoản không gắn nhân viên chỉ có thể là Admin");
        }
        jdbc.update("UPDATE user_account SET role = ?, token_version = token_version + 1 WHERE id = ?", role, id);
        if ("ADMIN".equals(role)) {
            jdbc.update("UPDATE payroll_grant SET revoked_at = now(), revoked_by = ?, revoke_reason = 'Đổi sang role Admin' WHERE account_id = ? AND revoked_at IS NULL",
                    admin.userId(), id);
        }
        audit.success("CHANGE_ROLE", "user_account", id, reason, Map.of("role", acc.get("role")), Map.of("role", role));
        return getAccount(id);
    }

    // ---------------- Đề nghị cấp tài khoản ----------------

    private static final String REQUEST_SELECT = """
            SELECT r.id, r.employee_id, e.code AS employee_code, e.full_name AS employee_name, e.work_email,
                   r.requested_role, r.reason, r.status, COALESCE(re.full_name, ra.email) AS requested_by_name,
                   r.created_at, r.process_note, r.created_account_id
            FROM account_request r JOIN employee e ON e.id = r.employee_id
            JOIN user_account ra ON ra.id = r.requested_by LEFT JOIN employee re ON re.id = ra.employee_id
            """;

    public List<Map<String, Object>> listRequests() {
        Principals.requireRole("ADMIN", "HR");
        return Sql.camel(jdbc.queryForList(REQUEST_SELECT + " ORDER BY r.id DESC"));
    }

    @Transactional
    public Map<String, Object> createRequest(long employeeId, String role, String reason) {
        AuthUser hr = Principals.requireRole("HR");
        requireRole(role);
        if ("ADMIN".equals(role)) {
            throw BusinessException.unprocessable("ADMIN_NOT_EMPLOYEE", "Không đề nghị role Admin cho nhân viên");
        }
        Integer emp = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = ? AND status <> 'TERMINATED'", Integer.class, employeeId);
        if (emp == null || emp == 0) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên đang làm việc");
        }
        Integer open = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM user_account WHERE employee_id = ?) +
                       (SELECT COUNT(*) FROM account_request WHERE employee_id = ? AND status = 'PENDING')
                """, Integer.class, employeeId, employeeId);
        if (open != null && open > 0) {
            throw BusinessException.conflict("ACCOUNT_EXISTS", "Nhân viên đã có tài khoản hoặc đề nghị đang chờ xử lý");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO account_request(employee_id, requested_role, reason, status, requested_by)
                VALUES (?, ?, ?, 'PENDING', ?) RETURNING id
                """, Long.class, employeeId, role, reason, hr.userId());
        audit.success("CREATE_ACCOUNT_REQUEST", "account_request", id, reason, null, Map.of("employeeId", employeeId, "role", role));
        return Sql.camel(jdbc.queryForMap(REQUEST_SELECT + " WHERE r.id = ?", id));
    }

    @Transactional
    public Map<String, Object> processRequest(long id, String email, String tempPassword, String note) {
        Principals.requireRole("ADMIN");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.employee_id, r.requested_role, r.status, e.work_email FROM account_request r JOIN employee e ON e.id = r.employee_id WHERE r.id = ? FOR UPDATE OF r", id);
        if (rows.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy đề nghị");
        }
        Map<String, Object> r = rows.get(0);
        if (!"PENDING".equals(r.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Đề nghị đã được xử lý");
        }
        String mail = email == null || email.isBlank() ? (String) r.get("work_email") : email;
        Map<String, Object> created = createAccount(((Number) r.get("employee_id")).longValue(), mail,
                (String) r.get("requested_role"), tempPassword, note == null ? "Xử lý đề nghị #" + id : note, id);
        jdbc.update("UPDATE account_request SET process_note = ? WHERE id = ?", note, id);
        Map<String, Object> out = new LinkedHashMap<>(created);
        out.put("request", Sql.camel(jdbc.queryForMap(REQUEST_SELECT + " WHERE r.id = ?", id)));
        return out;
    }

    @Transactional
    public Map<String, Object> rejectRequest(long id, String reason) {
        AuthUser admin = Principals.requireRole("ADMIN");
        int n = jdbc.update("""
                UPDATE account_request SET status = 'REJECTED', processed_by = ?, processed_at = now(), process_note = ?
                WHERE id = ? AND status = 'PENDING'
                """, admin.userId(), reason, id);
        if (n == 0) {
            throw BusinessException.conflict("INVALID_STATE", "Đề nghị không tồn tại hoặc đã được xử lý");
        }
        audit.success("REJECT_ACCOUNT_REQUEST", "account_request", id, reason, null, null);
        return Sql.camel(jdbc.queryForMap(REQUEST_SELECT + " WHERE r.id = ?", id));
    }

    // ---------------- Grant payroll ----------------

    private static final String GRANT_SELECT = """
            SELECT g.id, g.account_id, a.email AS account_email, COALESCE(e.full_name, a.email) AS holder_name,
                   g.permission, g.period_id, g.valid_from, g.valid_to, g.reason, g.decision_ref,
                   COALESCE(ge.full_name, ga.email) AS granted_by_name, g.revoked_at, g.revoke_reason,
                   (g.revoked_at IS NULL AND g.valid_from <= now() AND (g.valid_to IS NULL OR g.valid_to > now())) AS active
            FROM payroll_grant g JOIN user_account a ON a.id = g.account_id LEFT JOIN employee e ON e.id = a.employee_id
            JOIN user_account ga ON ga.id = g.granted_by LEFT JOIN employee ge ON ge.id = ga.employee_id
            """;

    public List<Map<String, Object>> listGrants() {
        Principals.requireRole("ADMIN");
        return Sql.camel(jdbc.queryForList(GRANT_SELECT + " ORDER BY g.id"));
    }

    @Transactional
    public Map<String, Object> createGrant(long accountId, String permission, Long periodId, Instant validFrom,
                                           Instant validTo, String reason, String decisionRef) {
        AuthUser admin = Principals.requireRole("ADMIN");
        if (!PERMS.contains(permission)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Quyền payroll không hợp lệ");
        }
        String role = jdbc.query("SELECT role FROM user_account WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, accountId);
        if (role == null) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản");
        }
        if ("ADMIN".equals(role)) {
            throw BusinessException.unprocessable("ADMIN_CANNOT_HOLD_GRANT",
                    "Admin kỹ thuật không được giữ quyền lương (permission_matrix Mục 3.4)");
        }
        Instant from = validFrom == null ? Instant.now() : validFrom;
        if (validTo != null && !validTo.isAfter(from)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "validTo phải sau validFrom");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO payroll_grant(account_id, permission, period_id, valid_from, valid_to, reason, decision_ref, granted_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, accountId, permission, periodId, Timestamp.from(from),
                validTo == null ? null : Timestamp.from(validTo), reason, decisionRef, admin.userId());
        audit.success("CREATE_GRANT", "payroll_grant", id, reason, null,
                Map.of("accountId", accountId, "permission", permission, "decisionRef", decisionRef));
        return Sql.camel(jdbc.queryForMap(GRANT_SELECT + " WHERE g.id = ?", id));
    }

    @Transactional
    public Map<String, Object> revokeGrant(long id, String reason) {
        AuthUser admin = Principals.requireRole("ADMIN");
        int n = jdbc.update("UPDATE payroll_grant SET revoked_at = now(), revoked_by = ?, revoke_reason = ? WHERE id = ? AND revoked_at IS NULL",
                admin.userId(), reason, id);
        if (n == 0) {
            throw BusinessException.conflict("INVALID_STATE", "Grant không tồn tại hoặc đã thu hồi");
        }
        audit.success("REVOKE_GRANT", "payroll_grant", id, reason, null, null);
        return Sql.camel(jdbc.queryForMap(GRANT_SELECT + " WHERE g.id = ?", id));
    }

    // ---------------- Tuyến duyệt ----------------

    private static final String ROUTE_SELECT = """
            SELECT r.id, r.requester_id, e.code AS requester_code, e.full_name AS requester_name,
                   r.primary_approver_id, p.full_name AS primary_approver_name,
                   r.backup_approver_id, b.full_name AS backup_approver_name, r.backup_designation_ref,
                   r.effective_from, r.effective_to, r.note
            FROM approval_route r JOIN employee e ON e.id = r.requester_id
            LEFT JOIN employee p ON p.id = r.primary_approver_id LEFT JOIN employee b ON b.id = r.backup_approver_id
            WHERE r.effective_from <= current_date AND (r.effective_to IS NULL OR r.effective_to >= current_date)
            """;

    public List<Map<String, Object>> listRoutes() {
        Principals.requireRole("ADMIN", "HR");
        return Sql.camel(jdbc.queryForList(ROUTE_SELECT + " ORDER BY e.code"));
    }

    @Transactional
    public Map<String, Object> updateRoute(long requesterId, Long primaryId, Long backupId, Long designatedBy,
                                           String designationRef, LocalDate effectiveFrom, String note) {
        Principals.requireRole("ADMIN", "HR");
        Integer exists = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = ?", Integer.class, requesterId);
        if (exists == null || exists == 0) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
        if (requesterId == nz(primaryId) || requesterId == nz(backupId)) {
            throw BusinessException.unprocessable("SELF_APPROVER", "Người duyệt không được là chính nhân viên đó");
        }
        if (primaryId != null && primaryId.equals(backupId)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Người dự phòng phải khác người duyệt chính");
        }
        for (Long approver : new Long[] {primaryId, backupId}) {
            if (approver != null) {
                Integer ok = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = ? AND status <> 'TERMINATED'", Integer.class, approver);
                if (ok == null || ok == 0) {
                    throw BusinessException.unprocessable("VALIDATION_ERROR", "Người duyệt không tồn tại hoặc đã nghỉ việc");
                }
            }
        }
        if (backupId != null && (designationRef == null || designationRef.isBlank())) {
            throw BusinessException.unprocessable("BACKUP_DESIGNATION_REQUIRED",
                    "Người dự phòng phải được chỉ định trước bằng văn bản/quyết định (backupDesignationRef)");
        }
        jdbc.update("""
                UPDATE approval_route SET effective_to = ? - 1 WHERE requester_id = ? AND effective_to IS NULL AND effective_from < ?
                """, effectiveFrom, requesterId, effectiveFrom);
        jdbc.update("DELETE FROM approval_route WHERE requester_id = ? AND effective_to IS NULL AND effective_from >= ?",
                requesterId, effectiveFrom);
        jdbc.update("""
                INSERT INTO approval_route(requester_id, primary_approver_id, backup_approver_id, backup_designated_by,
                                           backup_designation_ref, effective_from, note)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, requesterId, primaryId, backupId, designatedBy, designationRef, effectiveFrom, note);
        audit.success("UPDATE_APPROVAL_ROUTE", "approval_route", requesterId, note, null,
                Map.of("primary", String.valueOf(primaryId), "backup", String.valueOf(backupId)));
        return Sql.camel(jdbc.queryForMap(ROUTE_SELECT + " AND r.requester_id = ? ORDER BY r.id DESC LIMIT 1", requesterId));
    }

    // ---------------- Vắng mặt của người duyệt ----------------

    public List<Map<String, Object>> listUnavailability() {
        AuthUser u = Principals.current();
        String sql = """
                SELECT v.id, v.employee_id, e.full_name AS employee_name, v.from_date, v.to_date, v.reason, v.created_at
                FROM approver_unavailability v JOIN employee e ON e.id = v.employee_id
                """;
        if (u.is("HR", "ADMIN")) {
            return Sql.camel(jdbc.queryForList(sql + " ORDER BY v.from_date DESC"));
        }
        return Sql.camel(jdbc.queryForList(sql + " WHERE v.employee_id = ? ORDER BY v.from_date DESC", Principals.employeeId()));
    }

    @Transactional
    public Map<String, Object> createUnavailability(long employeeId, LocalDate from, LocalDate to, String reason) {
        AuthUser u = Principals.current();
        if (!u.is("HR") && !(u.employeeId() != null && u.employeeId() == employeeId)) {
            throw new org.springframework.security.access.AccessDeniedException("Chỉ HR hoặc chính người duyệt được khai báo");
        }
        if (to.isBefore(from)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "toDate phải từ fromDate trở đi");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO approver_unavailability(employee_id, from_date, to_date, reason, created_by)
                VALUES (?, ?, ?, ?, ?) RETURNING id
                """, Long.class, employeeId, from, to, reason, u.userId());
        audit.success("DECLARE_UNAVAILABLE", "approver_unavailability", id, reason, null, null);
        return Sql.camel(jdbc.queryForMap("""
                SELECT v.id, v.employee_id, e.full_name AS employee_name, v.from_date, v.to_date, v.reason, v.created_at
                FROM approver_unavailability v JOIN employee e ON e.id = v.employee_id WHERE v.id = ?
                """, id));
    }

    // ---------------- helpers ----------------

    private static long nz(Long v) {
        return v == null ? Long.MIN_VALUE : v;
    }

    private static void requireRole(String role) {
        if (role == null || !ROLES.contains(role)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Role không hợp lệ");
        }
    }

    static String generatePassword() {
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ";
        String lower = "abcdefghijkmnopqrstuvwxyz";
        String digits = "23456789";
        String special = "!@#$%^&*";
        String all = upper + lower + digits + special;
        SecureRandom rnd = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        sb.append(upper.charAt(rnd.nextInt(upper.length()))).append(lower.charAt(rnd.nextInt(lower.length())))
                .append(digits.charAt(rnd.nextInt(digits.length()))).append(special.charAt(rnd.nextInt(special.length())));
        for (int i = 0; i < 8; i++) {
            sb.append(all.charAt(rnd.nextInt(all.length())));
        }
        return sb.toString();
    }
}
