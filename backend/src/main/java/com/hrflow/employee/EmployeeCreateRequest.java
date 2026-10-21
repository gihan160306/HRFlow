package com.hrflow.employee;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record EmployeeCreateRequest(
        @NotBlank @Size(max = 20) String employeeCode,
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 255) String workEmail,
        @Size(max = 20) String phone,
        @Past LocalDate dateOfBirth,
        @Size(max = 255) String address,
        @Pattern(regexp = "^[0-9]{12}$", message = "CCCD phải đúng 12 chữ số") String nationalId,
        @Pattern(regexp = "^[0-9]{10}(-[0-9]{3})?$", message = "Mã số thuế phải gồm 10 số hoặc 10-3 số") String taxCode,
        @Size(max = 100) String bankName,
        @Size(max = 30) String bankAccountNo,
        @NotNull Long departmentId,
        @NotBlank @Size(max = 100) String jobTitle,
        @NotNull Employee.EmployeeType employeeType,
        Long managerId,
        @NotNull LocalDate hireDate,
        @Min(0) @Max(30) Integer dependentsCount,
        Boolean taxCommitment08,
        Boolean otEligible,
        Boolean commissionEligible,
        Boolean kpiEligible,
        Boolean performanceBonusEligible,
        Boolean teamBonusEligible,
        Boolean lunchAllowanceEligible,
        Boolean parkingAllowanceEligible,
        Boolean phoneAllowanceEligible,
        Boolean positionAllowanceEligible) {
}