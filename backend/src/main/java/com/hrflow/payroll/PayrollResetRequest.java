package com.hrflow.payroll;

import jakarta.validation.constraints.NotBlank;

public record PayrollResetRequest(@NotBlank String reason) { }
