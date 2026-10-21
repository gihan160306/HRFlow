package com.hrflow.attendance;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import com.hrflow.common.audit.AuditableEntity;
import com.hrflow.employee.Employee;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Bảng công theo ngày — khớp bảng attendance (data_dictionary.md §3.3). UQ (employee_id, work_date). */
@Entity
@Table(name = "attendance",
        uniqueConstraints = @UniqueConstraint(name = "uq_attendance_employee_date", columnNames = {"employee_id", "work_date"}))
public class Attendance extends AuditableEntity {

    public enum Source { WEB, QR, MANAGER_CONFIRMED, HR_CORRECTION }

    public enum ExceptionDecision { FULL, HALF_PAID, HALF_UNPAID, UNPAID }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendance_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false, updatable = false)
    private Employee employee;

    /** Ngày công theo múi giờ Asia/Ho_Chi_Minh. */
    @Column(name = "work_date", nullable = false, updatable = false)
    private LocalDate workDate;

    @Column(name = "check_in_at")
    private Instant checkInAt;

    @Column(name = "check_out_at")
    private Instant checkOutAt;

    @Column(name = "late_minutes", nullable = false)
    private int lateMinutes;

    @Column(name = "early_minutes", nullable = false)
    private int earlyMinutes;

    @Column(name = "worked_minutes", nullable = false)
    private int workedMinutes;

    @Column(name = "day_credit", nullable = false, precision = 2, scale = 1)
    private BigDecimal dayCredit = new BigDecimal("0.0");

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private AttendanceStatus status = AttendanceStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 20)
    private Source source = Source.WEB;

    @Enumerated(EnumType.STRING)
    @Column(name = "exception_decision", length = 12)
    private ExceptionDecision exceptionDecision;

    @Column(name = "decided_by")
    private Long decidedBy;

    @Column(name = "decided_at")
    private Instant decidedAt;

    @Column(name = "decision_reason", length = 500)
    private String decisionReason;

    @Column(name = "confirmed_by")
    private Long confirmedBy;

    @Column(name = "note", length = 500)
    private String note;

    public Long getId() { return id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee v) { this.employee = v; }
    public LocalDate getWorkDate() { return workDate; }
    public void setWorkDate(LocalDate v) { this.workDate = v; }
    public Instant getCheckInAt() { return checkInAt; }
    public void setCheckInAt(Instant v) { this.checkInAt = v; }
    public Instant getCheckOutAt() { return checkOutAt; }
    public void setCheckOutAt(Instant v) { this.checkOutAt = v; }
    public int getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(int v) { this.lateMinutes = v; }
    public int getEarlyMinutes() { return earlyMinutes; }
    public void setEarlyMinutes(int v) { this.earlyMinutes = v; }
    public int getWorkedMinutes() { return workedMinutes; }
    public void setWorkedMinutes(int v) { this.workedMinutes = v; }
    public BigDecimal getDayCredit() { return dayCredit; }
    public void setDayCredit(BigDecimal v) { this.dayCredit = v; }
    public AttendanceStatus getStatus() { return status; }
    public void setStatus(AttendanceStatus v) { this.status = v; }
    public Source getSource() { return source; }
    public void setSource(Source v) { this.source = v; }
    public ExceptionDecision getExceptionDecision() { return exceptionDecision; }
    public void setExceptionDecision(ExceptionDecision v) { this.exceptionDecision = v; }
    public Long getDecidedBy() { return decidedBy; }
    public void setDecidedBy(Long v) { this.decidedBy = v; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant v) { this.decidedAt = v; }
    public String getDecisionReason() { return decisionReason; }
    public void setDecisionReason(String v) { this.decisionReason = v; }
    public Long getConfirmedBy() { return confirmedBy; }
    public void setConfirmedBy(Long v) { this.confirmedBy = v; }
    public String getNote() { return note; }
    public void setNote(String v) { this.note = v; }
}