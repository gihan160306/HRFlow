package com.hrflow.employee;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record EmployeeResponse(
        Long employeeId,
        String employeeCode,
        String fullName,
        String workEmail,
        String phone,
        LocalDate dateOfBirth,
        String address,
        String nationalId,
        String taxCode,
        String bankName,
        String bankAccountNo,
        Long departmentId,
        String departmentName,
        String jobTitle,
        Employee.EmployeeType employeeType,
        Long managerId,
        LocalDate hireDate,
        Employee.Status status,
        int dependentsCount,
        boolean taxCommitment08,
        boolean otEligible,
        boolean commissionEligible,
        boolean kpiEligible,
        boolean performanceBonusEligible,
        boolean teamBonusEligible,
        boolean lunchAllowanceEligible,
        boolean parkingAllowanceEligible,
        boolean phoneAllowanceEligible,
        boolean positionAllowanceEligible,
        Instant createdAt,
        Instant updatedAt) {

    /** @param includeSensitive true chỉ khi người xem là HR/Admin (CCCD, MST, ngân hàng). */
    public static EmployeeResponse from(Employee e, boolean includeSensitive) {
        return new EmployeeResponse(
                e.getId(),
                e.getEmployeeCode(),
                e.getFullName(),
                e.getWorkEmail(),
                e.getPhone(),
                e.getDateOfBirth(),
                e.getAddress(),
                includeSensitive ? e.getNationalId() : null,
                includeSensitive ? e.getTaxCode() : null,
                includeSensitive ? e.getBankName() : null,
                includeSensitive ? e.getBankAccountNo() : null,
                e.getDepartment().getId(),
                e.getDepartment().getDepartmentName(),
                e.getJobTitle(),
                e.getEmployeeType(),
                e.getManagerId(),
                e.getHireDate(),
                e.getStatus(),
                e.getDependentsCount(),
                e.isTaxCommitment08(),
                e.isOtEligible(),
                e.isCommissionEligible(),
                e.isKpiEligible(),
                e.isPerformanceBonusEligible(),
                e.isTeamBonusEligible(),
                e.isLunchAllowanceEligible(),
                e.isParkingAllowanceEligible(),
                e.isPhoneAllowanceEligible(),
                e.isPositionAllowanceEligible(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}