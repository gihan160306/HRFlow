
package com.hrflow.payroll;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PayrollCalculationRequest(
        @NotNull
        @Positive
        Long periodId
) {
}
