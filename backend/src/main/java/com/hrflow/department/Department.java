package com.hrflow.department;

import com.hrflow.common.audit.AuditableEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Phòng ban — khớp bảng department trong data_dictionary.md §3.1. Không xóa, chỉ ngừng hoạt động. */
@Entity
@Table(name = "department")
public class Department extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "department_id")
    private Long id;

    /** Mã ngắn, không đổi sau khi tạo (ví dụ CONTENT, ADS). */
    @Column(name = "department_code", nullable = false, unique = true, length = 20, updatable = false)
    private String departmentCode;

    @Column(name = "department_name", nullable = false, unique = true, length = 100)
    private String departmentName;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String v) { this.departmentCode = v; }
    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String v) { this.departmentName = v; }
    public String getDescription() { return description; }
    public void setDescription(String v) { this.description = v; }
    public boolean isActive() { return active; }
    public void setActive(boolean v) { this.active = v; }
}