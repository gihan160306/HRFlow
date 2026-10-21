package com.hrflow.attendance;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** HR điều chỉnh giờ vào/ra. Trường giờ để trống nghĩa là giữ nguyên. Lý do bắt buộc. */
public record AttendanceAdjustRequest(
        @NotNull Long attendanceId,
        OffsetDateTime checkInAt,
        OffsetDateTime checkOutAt,
        @NotBlank @Size(min = 3, max = 500) String reason) {
}