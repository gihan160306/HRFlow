package com.hrflow.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record DepartmentCreateRequest(
        @NotBlank @Size(max = 20)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Mã phòng ban chỉ gồm chữ, số, '_' hoặc '-'")
        String departmentCode,
        @NotBlank @Size(max = 100) String departmentName,
        @Size(max = 255) String description) {
}