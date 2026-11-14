package com.hrflow.common.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/** approval_log dùng chung cho mọi loại đơn (append-only). */
@Service
public class ApprovalLogService {

    private final JdbcTemplate jdbc;

    public ApprovalLogService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void log(String entityType, long entityId, String action, String from, String to, String reason,
                    boolean usedBackup) {
        AuthUser u = Principals.currentOrNull();
        String actorName = u == null ? null : Principals.displayName(jdbc, u.userId());
        jdbc.update("""
                INSERT INTO approval_log(entity_type, entity_id, action, actor_account_id, actor_name, from_status,
                                         to_status, reason, used_backup)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, entityType, entityId, action, u == null ? null : u.userId(), actorName, from, to, reason, usedBackup);
    }
}
