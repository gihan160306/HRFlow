package com.hrflow.overtime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
public class OvertimeController {

    public record ScheduleRequest(@NotBlank String title, @NotNull LocalDate workDate, @NotNull LocalTime startTime,
                                  @NotNull LocalTime endTime, String description, Long departmentId,
                                  @NotEmpty List<Long> targetEmployeeIds) {
    }

    public record RegisterRequest(@NotNull Long scheduleId, @NotNull Boolean consent, LocalTime startTime, LocalTime endTime) {
    }

    public record ApproveRequest(Integer approvedMinutes, String reason, String backupReason) {
    }

    public record ActualRequest(@NotBlank String status, Instant actualStart, Instant actualEnd, Boolean extraConfirmed,
                                String evidence) {
    }

    public record UnscheduledRequest(Long employeeId, @NotNull Instant startAt, @NotNull Instant endAt,
                                     @NotBlank String description, String evidence) {
    }

    public record VerifyRequest(String note) {
    }

    private final OvertimeService service;

    public OvertimeController(OvertimeService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/ot-schedules")
    public List<Map<String, Object>> schedules() {
        return service.schedules();
    }

    @PostMapping("/api/v1/ot-schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createSchedule(@Valid @RequestBody ScheduleRequest r) {
        return service.createSchedule(new OvertimeService.ScheduleCmd(r.title(), r.workDate(), r.startTime(), r.endTime(),
                r.description(), r.departmentId(), r.targetEmployeeIds()));
    }

    @PostMapping("/api/v1/ot-schedules/{id}/publish")
    public Map<String, Object> publish(@PathVariable long id) {
        return service.publish(id);
    }

    @PostMapping("/api/v1/ot-schedules/{id}/close")
    public Map<String, Object> close(@PathVariable long id) {
        return service.closeSchedule(id);
    }

    @PostMapping("/api/v1/ot-schedules/{id}/cancel")
    public Map<String, Object> cancelSchedule(@PathVariable long id) {
        return service.cancelSchedule(id);
    }

    @GetMapping("/api/v1/ot-registrations")
    public List<Map<String, Object>> registrations() {
        return service.registrations();
    }

    @GetMapping("/api/v1/ot-registrations/me")
    public List<Map<String, Object>> mine() {
        return service.myRegistrations();
    }

    @GetMapping("/api/v1/ot-registrations/inbox")
    public List<Map<String, Object>> inbox() {
        return service.inbox();
    }

    @PostMapping("/api/v1/ot-registrations")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> register(@Valid @RequestBody RegisterRequest r) {
        return service.register(new OvertimeService.RegisterCmd(r.scheduleId(), r.consent(), r.startTime(), r.endTime()));
    }

    @PostMapping("/api/v1/ot-registrations/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) ApproveRequest r) {
        ApproveRequest a = r == null ? new ApproveRequest(null, null, null) : r;
        return service.decide(id, true, a.approvedMinutes(), a.reason(), a.backupReason());
    }

    @PostMapping("/api/v1/ot-registrations/{id}/reject")
    public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) ApproveRequest r) {
        ApproveRequest a = r == null ? new ApproveRequest(null, null, null) : r;
        return service.decide(id, false, null, a.reason(), a.backupReason());
    }

    @PostMapping("/api/v1/ot-registrations/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    @PostMapping("/api/v1/ot-registrations/{id}/actual")
    public Map<String, Object> actual(@PathVariable long id, @Valid @RequestBody ActualRequest r) {
        return service.confirmActual(id, new OvertimeService.ActualCmd(r.status(), r.actualStart(), r.actualEnd(),
                r.extraConfirmed() != null && r.extraConfirmed(), r.evidence()));
    }

    @PostMapping("/api/v1/ot-registrations/{id}/hr-check")
    public Map<String, Object> hrCheck(@PathVariable long id) {
        return service.hrCheck(id);
    }

    @GetMapping("/api/v1/ot-unscheduled")
    public List<Map<String, Object>> unscheduled() {
        return service.unscheduled();
    }

    @PostMapping("/api/v1/ot-unscheduled")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createUnscheduled(@Valid @RequestBody UnscheduledRequest r) {
        return service.createUnscheduled(r.employeeId(), r.startAt(), r.endAt(), r.description(), r.evidence());
    }

    @PostMapping("/api/v1/ot-unscheduled/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable long id, @RequestBody(required = false) VerifyRequest r) {
        return service.verifyUnscheduled(id, true, r == null ? null : r.note());
    }

    @PostMapping("/api/v1/ot-unscheduled/{id}/reject")
    public Map<String, Object> rejectUnscheduled(@PathVariable long id, @RequestBody(required = false) VerifyRequest r) {
        return service.verifyUnscheduled(id, false, r == null ? null : r.note());
    }
}
