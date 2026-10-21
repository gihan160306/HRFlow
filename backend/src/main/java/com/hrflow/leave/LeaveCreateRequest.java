package com.hrflow.leave;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
public record LeaveCreateRequest(
    @NotNull LocalDate startDate,
    @NotNull LocalDate endDate,
    @NotBlank String leaveType,
    String halfDaySession,
    @Size(max=500) String reason
) {}
