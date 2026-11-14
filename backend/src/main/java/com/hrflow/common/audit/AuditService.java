package com.hrflow.common.audit;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.util.Json;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/**
 * Ghi audit_log (append-only) trong giao dịch riêng để vẫn lưu được sự kiện bảo mật (DENIED)
 * khi giao dịch nghiệp vụ bị rollback.
 */
@Service
public class AuditService {

    private final JdbcTemplate jdbc;
    private final Json json;

    public AuditService(JdbcTemplate jdbc, Json json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String entityType, Object entityId, String result, String reason,
                       Object before, Object after) {
        AuthUser u = Principals.currentOrNull();
        jdbc.update("""
                INSERT INTO audit_log(actor_account_id, actor_name, action, entity_type, entity_id, result, reason,
                                      before_json, after_json)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                """, u == null ? null : u.userId(), u == null ? null : u.email(), action, entityType,
                entityId == null ? null : String.valueOf(entityId), result, reason,
                before == null ? null : json.write(before), after == null ? null : json.write(after));
    }

    public void success(String action, String entityType, Object entityId, String reason, Object before, Object after) {
        record(action, entityType, entityId, "SUCCESS", reason, before, after);
    }

    public void denied(String action, String entityType, Object entityId, String reason) {
        record(action, entityType, entityId, "DENIED", reason, null, null);
    }
}
