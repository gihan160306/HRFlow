package com.hrflow.leave;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
public record LeaveResponse(Long requestId,Long employeeId,LocalDate startDate,LocalDate endDate,String leaveType,String halfDaySession,BigDecimal durationDays,String reason,LeaveStatus status,OffsetDateTime submittedAt,Long decidedBy,OffsetDateTime decidedAt,String decisionReason,OffsetDateTime createdAt){
 public static LeaveResponse from(LeaveRequest l){return new LeaveResponse(l.getId(),l.getEmployeeId(),l.getStartDate(),l.getEndDate(),l.getLeaveType(),l.getHalfDaySession(),l.getDurationDays(),l.getReason(),l.getStatus(),l.getSubmittedAt(),l.getDecidedBy(),l.getDecidedAt(),l.getDecisionReason(),l.getCreatedAt());}
}
