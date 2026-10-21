
package com.hrflow.payslip;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PayslipResponse(
        Long payslipId,
        Long employeeId,
        Long periodId,
        String periodCode,
        LocalDate startDate,
        LocalDate endDate,
        Integer standardWorkingDays,
        String status,
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
