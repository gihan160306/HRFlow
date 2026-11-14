package com.hrflow.security;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.hrflow.common.audit.AuditService;

/**
 * Quyền payroll (VIEW/PREPARE/APPROVE/LOCK) là grant riêng theo tài khoản trong payroll_grant: còn hiệu lực
 * (valid_from/valid_to), chưa thu hồi, đúng kỳ (nếu grant giới hạn kỳ). Role/chức danh không tự cấp grant,
 * và tài khoản Admin không bao giờ có quyền lương dù có bản ghi grant (permission_matrix Mục 3.4).
 */
@Service("payrollAccess")
public class PayrollAccess {

    private final JdbcTemplate jdbc;
    private final AuditService audit;

    public PayrollAccess(JdbcTemplate jdbc, AuditService audit) {
        this.jdbc = jdbc;
        this.audit = audit;
    }

    public boolean has(long accountId, String permission, Long periodId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payroll_grant g JOIN user_account a ON a.id = g.account_id
                WHERE g.account_id = ? AND g.permission = ? AND a.status = 'ACTIVE' AND a.role <> 'ADMIN'
                  AND g.revoked_at IS NULL AND g.valid_from <= now()
                  AND (g.valid_to IS NULL OR g.valid_to > now())
                  AND (g.period_id IS NULL OR g.period_id = ?)
                """, Integer.class, accountId, permission, periodId);
        return n != null && n > 0;
    }

    public boolean has(String permission, Long periodId) {
        AuthUser u = Principals.currentOrNull();
        return u != null && has(u.userId(), permission, periodId);
    }

    public boolean hasAny(Long periodId, String... permissions) {
        for (String p : permissions) {
            if (has(p, periodId)) {
                return true;
            }
        }
        return false;
    }

    /** Ném 403 (và ghi audit DENIED) nếu thiếu grant. */
    public AuthUser require(String permission, Long periodId) {
        AuthUser u = Principals.current();
        if (!has(u.userId(), permission, periodId)) {
            audit.denied("PAYROLL_ACCESS", "payroll_grant", periodId, "Thiếu grant " + permission);
            throw new AccessDeniedException("Thiếu quyền payroll " + permission);
        }
        return u;
    }

    public AuthUser requireAny(Long periodId, String... permissions) {
        AuthUser u = Principals.current();
        for (String p : permissions) {
            if (has(u.userId(), p, periodId)) {
                return u;
            }
        }
        audit.denied("PAYROLL_ACCESS", "payroll_grant", periodId, "Thiếu grant " + String.join("/", permissions));
        throw new AccessDeniedException("Thiếu quyền payroll");
    }

    public List<String> activePermissions(long accountId) {
        return jdbc.queryForList("""
                SELECT DISTINCT g.permission FROM payroll_grant g JOIN user_account a ON a.id = g.account_id
                WHERE g.account_id = ? AND a.status = 'ACTIVE' AND a.role <> 'ADMIN' AND g.revoked_at IS NULL
                  AND g.valid_from <= now() AND (g.valid_to IS NULL OR g.valid_to > now()) ORDER BY 1
                """, String.class, accountId);
    }
}
