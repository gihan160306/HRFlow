package com.hrflow.common.audit;

import java.time.Instant;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

/**
 * Lớp cha cho các entity có cột created_at / updated_at (TIMESTAMPTZ NOT NULL DEFAULT now()).
 *
 * <p>Không dùng cho bảng append-only không có updated_at
 * (audit_log, approval_log, notification, payroll_line...).
 * Các cột người thực hiện (created_by, approved_by...) khai báo riêng ở từng entity.
 */
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
