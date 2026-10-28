
package com.hrflow.payslip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PayslipResponse(
        Long payslipId,
        Long employeeId,
        String employeeCode,
        String fullName,
        Long periodId,
        String periodCode,
        LocalDate startDate,
        LocalDate endDate,
        Integer standardWorkingDays,
        String status,
        String policyVersion,
        BigDecimal grossSalary,
        BigDecimal bhxhAmount,
        BigDecimal bhytAmount,
        BigDecimal bhtnAmount,
        BigDecimal totalInsurance,
        BigDecimal pitAmount,
        BigDecimal otherDeductions,
        BigDecimal periodAdjustments,
        BigDecimal carriedDebt,
        BigDecimal netSalary,
        BigDecimal netEntitlement,
        BigDecimal advanceRecovery,
        BigDecimal reimbursements,
        LocalDateTime createdAt,
        List<PayslipLine> lines
) {
    public record PayslipLine(
            Long lineId,
            String category,
            String label,
            BigDecimal amount,
            String calculationNote
    ) {
    }
}
