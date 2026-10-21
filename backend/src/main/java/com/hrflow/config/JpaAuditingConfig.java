package com.hrflow.config;

import java.time.Instant;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Bật JPA Auditing cho created_at / updated_at (TIMESTAMPTZ, lưu UTC).
 *
 * <p>Dùng {@link Instant} để khớp kiểu TIMESTAMPTZ trong data_dictionary.md.
 * Các cột người thực hiện (created_by, approved_by...) tham chiếu app_user.user_id
 * và được gán tường minh ở tầng service, nên không dùng AuditorAware ở đây.
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaAuditingConfig {

    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider auditingDateTimeProvider() {
        return () -> Optional.of(Instant.now());
    }
}
