package com.hrflow.contract;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ContractTerminateRequest(
        @NotNull LocalDate terminationDate,
        @NotBlank @Size(min = 3, max = 300) String reason) {
}