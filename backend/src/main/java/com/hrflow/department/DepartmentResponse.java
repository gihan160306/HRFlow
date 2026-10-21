package com.hrflow.department;

import java.time.Instant;

public record DepartmentResponse(
        Long departmentId,
        String departmentCode,
        String departmentName,
        String description,
        boolean active,
        Instant createdAt) {

    public static DepartmentResponse from(Department d) {
        return new DepartmentResponse(d.getId(), d.getDepartmentCode(), d.getDepartmentName(),
                d.getDescription(), d.isActive(), d.getCreatedAt());
    }
}