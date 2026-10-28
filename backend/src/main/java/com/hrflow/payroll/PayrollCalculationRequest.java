package com.hrflow.payroll;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** HR-confirmed inputs; the complete source snapshot is saved with each payslip. */
public record PayrollCalculationRequest(@NotNull @Positive Long periodId,
        @NotEmpty List<@Valid EmployeeInput> employees) {
    public record EmployeeInput(@NotNull @Positive Long employeeId, @NotBlank String sourceReference,
            @NotEmpty List<@Valid SalarySegment> salarySegments,
            @NotNull List<@Valid AllowanceInput> allowances, @NotNull List<@Valid OvertimeInput> overtime,
            @NotNull List<@Valid NightWorkInput> nightWork,
            @NotNull BigDecimal commission, @NotNull BigDecimal bonus,
            @NotNull BigDecimal otherTaxableIncome, @NotNull @Valid InsuranceInput insurance,
            @NotNull @Valid TaxInput tax, @NotNull BigDecimal legalDeductions,
            @NotNull BigDecimal advanceRecovery, @NotNull BigDecimal reimbursements,
            @NotNull BigDecimal netAdjustment) { }
    /** Full-month H_std is used even when employment begins or ends mid-month. */
    public record SalarySegment(@NotBlank String sourceReference, @NotNull BigDecimal monthlySalary,
            @NotNull BigDecimal standardHours, @NotNull BigDecimal paidHours) { }
    public record AllowanceInput(@NotBlank String code, @NotNull BigDecimal monthlyAmount,
            @NotNull BigDecimal standardHours, @NotNull BigDecimal eligibleHours,
            @NotNull Boolean taxable) { }
    public record OvertimeInput(@NotBlank String sourceReference, @NotNull BigDecimal hourlyRate,
            @NotNull BigDecimal hours, @NotNull BigDecimal dayMultiplier,
            @NotNull BigDecimal nightDayMultiplier, @NotNull Boolean night,
            @NotNull Boolean taxExempt) { }
    /** Ordinary night hours are already paid in salary; only the 30% premium is added. */
    public record NightWorkInput(@NotBlank String sourceReference, @NotNull BigDecimal hourlyRate,
            @NotNull BigDecimal hours, @NotNull Boolean taxExempt) { }
    public record InsuranceInput(@NotNull BigDecimal bhxhBase, @NotNull BigDecimal bhytBase,
            @NotNull BigDecimal bhtnBase, @NotNull Boolean bhxhParticipating,
            @NotNull Boolean bhytParticipating, @NotNull Boolean bhtnParticipating) { }
    public enum TaxMethod { PROGRESSIVE, SHORT_TERM, NON_RESIDENT }
    public record TaxInput(@NotNull TaxMethod method, @NotNull Integer validDependents,
            @NotNull BigDecimal otherLegalRelief, @NotNull Boolean validShortTermCommitment) { }
}
