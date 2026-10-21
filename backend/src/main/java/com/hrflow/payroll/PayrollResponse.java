
package com.hrflow.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PayrollResponse(
        Long periodId,
        String periodCode,
        LocalDate startDate,
        LocalDate endDate,
        PayrollStatus status,
        int employeeCount,
        List<PayrollRecordResponse> records
) {
    public record PayrollRecordResponse(
            Long payrollRecordId,
            Long employeeId,
            BigDecimal grossSalary,
            BigDecimal bhxhAmount,
            BigDecimal bhytAmount,
            BigDecimal bhtnAmount,
            BigDecimal pitAmount,
            BigDecimal otherDeductions,
            BigDecimal netSalary
    ) {
    }
}
