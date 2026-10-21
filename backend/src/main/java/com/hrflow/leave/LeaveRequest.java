package com.hrflow.leave;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
@Entity
@Table(name="leave_request")
public class LeaveRequest {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) @Column(name="leave_request_id") private Long id;
 @Column(name="employee_id",nullable=false) private Long employeeId;
 @Column(name="leave_type",nullable=false,length=16) private String leaveType;
 @Column(name="start_date",nullable=false) private LocalDate startDate;
 @Column(name="end_date",nullable=false) private LocalDate endDate;
 @Column(name="half_day_session",length=2) private String halfDaySession;
 @Column(name="duration_days",nullable=false,precision=4,scale=1) private BigDecimal durationDays;
 @Column(name="reason",length=500) private String reason;
 @Enumerated(EnumType.STRING) @Column(name="status",nullable=false,length=10) private LeaveStatus status=LeaveStatus.DRAFT;
 @Column(name="submitted_at") private OffsetDateTime submittedAt;
 @Column(name="decided_by") private Long decidedBy;
 @Column(name="decided_at") private OffsetDateTime decidedAt;
 @Column(name="decision_reason",length=500) private String decisionReason;
 @Column(name="created_at",insertable=false,updatable=false) private OffsetDateTime createdAt;
 protected LeaveRequest() {}
 public LeaveRequest(Long employeeId,String leaveType,LocalDate startDate,LocalDate endDate,String halfDaySession,BigDecimal durationDays,String reason){this.employeeId=employeeId;this.leaveType=leaveType;this.startDate=startDate;this.endDate=endDate;this.halfDaySession=halfDaySession;this.durationDays=durationDays;this.reason=reason;}
 public Long getId(){return id;} public Long getEmployeeId(){return employeeId;} public String getLeaveType(){return leaveType;} public LocalDate getStartDate(){return startDate;} public LocalDate getEndDate(){return endDate;} public String getHalfDaySession(){return halfDaySession;} public BigDecimal getDurationDays(){return durationDays;} public String getReason(){return reason;} public LeaveStatus getStatus(){return status;} public OffsetDateTime getSubmittedAt(){return submittedAt;} public Long getDecidedBy(){return decidedBy;} public OffsetDateTime getDecidedAt(){return decidedAt;} public String getDecisionReason(){return decisionReason;} public OffsetDateTime getCreatedAt(){return createdAt;}
 public void submit(OffsetDateTime now){status=LeaveStatus.PENDING;submittedAt=now;}
 public void cancel(){status=LeaveStatus.CANCELLED;}
 public void decide(LeaveStatus s,Long userId,OffsetDateTime now,String reason){status=s;decidedBy=userId;decidedAt=now;decisionReason=reason;}
}
