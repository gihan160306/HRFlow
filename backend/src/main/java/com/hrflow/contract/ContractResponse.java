package com.hrflow.contract;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ContractResponse(
        Long contractId,
        Long employeeId,
        String employeeCode,
        String fullName,
        String contractNo,
        Contract.ContractType contractType,
        LocalDate startDate,
        LocalDate endDate,
        Long baseSalary,
        Long monthlyStipend,
        BigDecimal probationRate,
        Long insuranceSalary,
        boolean probationInsured,
        ContractStatus status,
        Long renewedFromContractId,
        String note,
        Instant createdAt) {

    /** @param includeSalary true với HR/Admin hoặc chủ hợp đồng; Manager xem team thì ẩn các trường lương. */
    public static ContractResponse from(Contract c, boolean includeSalary) {
        return new ContractResponse(
                c.getId(),
                c.getEmployee().getId(),
                c.getEmployee().getEmployeeCode(),
                c.getEmployee().getFullName(),
                c.getContractNo(),
                c.getContractType(),
                c.getStartDate(),
                c.getEndDate(),
                includeSalary ? toLong(c.getBaseSalary()) : null,
                includeSalary ? toLong(c.getMonthlyStipend()) : null,
                includeSalary ? c.getProbationRate() : null,
                includeSalary ? toLong(c.getInsuranceSalary()) : null,
                c.isProbationInsured(),
                c.getStatus(),
                c.getRenewedFromContractId(),
                c.getNote(),
                c.getCreatedAt());
    }

    private static Long toLong(BigDecimal v) {
        return v == null ? null : v.longValue();
    }
}