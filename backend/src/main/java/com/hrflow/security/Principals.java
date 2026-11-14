package com.hrflow.security;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** Truy cập người dùng hiện tại. employee_id luôn lấy từ phiên, không tin client (permission_matrix Mục 1). */
public final class Principals {

    private Principals() {
    }

    public static AuthUser currentOrNull() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a != null && a.getPrincipal() instanceof AuthUser u) {
            return u;
        }
        return null;
    }

    public static AuthUser current() {
        AuthUser u = currentOrNull();
        if (u == null) {
            throw new AuthenticationCredentialsNotFoundException("Chưa đăng nhập");
        }
        return u;
    }

    /** Hồ sơ nhân viên của người đăng nhập; Admin kỹ thuật không có hồ sơ nên bị từ chối (403). */
    public static long employeeId() {
        Long id = current().employeeId();
        if (id == null) {
            throw new AccessDeniedException("Tài khoản không gắn hồ sơ nhân viên");
        }
        return id;
    }

    public static AuthUser requireRole(String... roles) {
        AuthUser u = current();
        if (!u.is(roles)) {
            throw new AccessDeniedException("Không đủ quyền");
        }
        return u;
    }

    public static String displayName(JdbcTemplate jdbc, Long accountId) {
        if (accountId == null) {
            return null;
        }
        return jdbc.query("""
                SELECT COALESCE(e.full_name, a.email) FROM user_account a
                LEFT JOIN employee e ON e.id = a.employee_id WHERE a.id = ?
                """, rs -> rs.next() ? rs.getString(1) : null, accountId);
    }
}
