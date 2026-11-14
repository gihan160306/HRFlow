package com.hrflow.common.audit;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/** Thông báo trong ứng dụng; dedupe_key chống tạo trùng. */
@Service
public class NotificationService {

    private final JdbcTemplate jdbc;

    public NotificationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void toAccount(Long accountId, String type, String title, String message, String link, String dedupeKey) {
        if (accountId == null) {
            return;
        }
        jdbc.update("""
                INSERT INTO notification(recipient_account_id, notification_type, title, message, link, dedupe_key)
                VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (dedupe_key) DO NOTHING
                """, accountId, type, title, message, link, dedupeKey);
    }

    public void toEmployee(Long employeeId, String type, String title, String message, String link, String dedupeKey) {
        if (employeeId == null) {
            return;
        }
        List<Long> ids = jdbc.queryForList("SELECT id FROM user_account WHERE employee_id = ? AND status = 'ACTIVE'",
                Long.class, employeeId);
        for (Long id : ids) {
            toAccount(id, type, title, message, link, dedupeKey == null ? null : dedupeKey + ":" + id);
        }
    }
}
