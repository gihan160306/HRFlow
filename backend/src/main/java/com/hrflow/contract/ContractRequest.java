package com.hrflow.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Tạo hợp đồng/phụ lục mới. Số tiền là VND, số nguyên. */
public record ContractRequest(
        @NotNull Long employeeId,
        @NotBlank @Size(max = 50) String contractNo,
        @NotNull Contract.ContractType contractType,
        @NotNull LocalDate startDate,
        LocalDate endDate,
        @PositiveOrZero Long baseSalary,
        @PositiveOrZero Long monthlyStipend,
        @DecimalMin(value = "0", inclusive = false) @DecimalMax("1") @Digits(integer = 1, fraction = 3)
        BigDecimal probationRate,
        @PositiveOrZero Long insuranceSalary,
        Boolean probationInsured,
        @Size(max = 500) String note) {
}