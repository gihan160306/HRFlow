package com.hrflow.department;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepartmentUpdateRequest(
        @NotBlank @Size(max = 100) String departmentName,
        @Size(max = 255) String description,
        Boolean active) {
}