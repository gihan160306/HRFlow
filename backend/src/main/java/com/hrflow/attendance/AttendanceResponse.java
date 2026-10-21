package com.hrflow.attendance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AttendanceResponse(
        Long attendanceId,
        Long employeeId,
        String employeeCode,
        String fullName,
        LocalDate workDate,
        Instant checkInAt,
        Instant checkOutAt,
        int lateMinutes,
        int earlyMinutes,
        int workedMinutes,
        BigDecimal dayCredit,
        AttendanceStatus status,
        Attendance.Source source,
        Attendance.ExceptionDecision exceptionDecision,
        String note) {

    public static AttendanceResponse from(Attendance a) {
        return new AttendanceResponse(
                a.getId(),
                a.getEmployee().getId(),
                a.getEmployee().getEmployeeCode(),
                a.getEmployee().getFullName(),
                a.getWorkDate(),
                a.getCheckInAt(),
                a.getCheckOutAt(),
                a.getLateMinutes(),
                a.getEarlyMinutes(),
                a.getWorkedMinutes(),
                a.getDayCredit(),
                a.getStatus(),
                a.getSource(),
                a.getExceptionDecision(),
                a.getNote());
    }
}