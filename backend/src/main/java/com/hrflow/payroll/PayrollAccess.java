package com.hrflow.payroll;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import com.hrflow.security.AuthUser;

/** Salary access is an explicit account grant; ADMIN and MANAGER roles grant nothing by themselves. */
@Service("payrollAccess")
public class PayrollAccess {
    private final JdbcTemplate jdbc;
    public PayrollAccess(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public boolean has(Authentication authentication, String permission) {
        return authentication != null && authentication.getPrincipal() instanceof AuthUser user
                && user.active() && hasUser(user.userId(), permission);
    }

    public boolean hasUser(Long userId, String permission) {
        if (userId == null) return false;
        Integer count = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payroll_permission p JOIN app_user u ON u.id = p.user_id
                WHERE p.user_id = ? AND p.permission = ? AND u.is_active = TRUE
                """, Integer.class, userId, permission);
        return count != null && count > 0;
    }
}
