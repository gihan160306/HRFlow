package com.hrflow.contract;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/** Gia hạn: các điều khoản tiền để trống thì giữ như hợp đồng cũ. */
public record ContractRenewRequest(
        @NotBlank @Size(max = 50) String contractNo,
        @NotNull LocalDate newStartDate,
        @NotNull LocalDate newEndDate,
        @PositiveOrZero Long baseSalary,
        @PositiveOrZero Long monthlyStipend,
        @PositiveOrZero Long insuranceSalary,
        @Size(max = 500) String note) {
}