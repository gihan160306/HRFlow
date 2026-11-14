package com.hrflow.leave;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
public class LeaveController {

    public record LeaveRequestBody(@NotNull Long leaveTypeId, @NotBlank String unit, @NotNull LocalDate startDate,
                                   @NotNull LocalDate endDate, LocalTime startTime, LocalTime endTime, String reason,
                                   String evidence) {
        LeaveService.Body toBody() {
            return new LeaveService.Body(leaveTypeId, unit, startDate, endDate, startTime, endTime, reason, evidence);
        }
    }

    public record DecisionRequest(String reason, String backupReason) {
    }

    public record BalanceAdjustRequest(@NotNull BigDecimal deltaDays, @NotBlank String reason) {
    }

    private final LeaveService service;

    public LeaveController(LeaveService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/leave-requests/preview")
    public Map<String, Object> preview(@Valid @RequestBody LeaveRequestBody r) {
        return service.preview(r.toBody());
    }

    @PostMapping("/api/v1/leave-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody LeaveRequestBody r, @RequestParam(defaultValue = "false") boolean submit) {
        return service.create(r.toBody(), submit);
    }

    @GetMapping("/api/v1/leave-requests")
    public List<Map<String, Object>> all(@RequestParam(required = false) String status, @RequestParam(required = false) Long employeeId) {
        return service.all(status, employeeId);
    }

    @GetMapping("/api/v1/leave-requests/me")
    public List<Map<String, Object>> mine() {
        return service.mine();
    }

    @GetMapping("/api/v1/leave-requests/inbox")
    public List<Map<String, Object>> inbox() {
        return service.inbox();
    }

    @GetMapping("/api/v1/leave-requests/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        return service.get(id);
    }

    @PutMapping("/api/v1/leave-requests/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody LeaveRequestBody r) {
        return service.update(id, r.toBody());
    }

    @PostMapping("/api/v1/leave-requests/{id}/submit")
    public Map<String, Object> submit(@PathVariable long id) {
        return service.submit(id);
    }

    @PostMapping("/api/v1/leave-requests/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id) {
        return service.cancel(id);
    }

    @PostMapping("/api/v1/leave-requests/{id}/cancel-request")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> cancelRequest(@PathVariable long id, @RequestBody(required = false) DecisionRequest r) {
        return service.requestCancel(id, r == null ? null : r.reason());
    }

    @PostMapping("/api/v1/leave-requests/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) DecisionRequest r) {
        DecisionRequest d = r == null ? new DecisionRequest(null, null) : r;
        return service.approve(id, d.reason(), d.backupReason());
    }

    @PostMapping("/api/v1/leave-requests/{id}/reject")
    public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) DecisionRequest r) {
        DecisionRequest d = r == null ? new DecisionRequest(null, null) : r;
        return service.reject(id, d.reason(), d.backupReason());
    }

    @GetMapping("/api/v1/leave-requests/{id}/logs")
    public List<Map<String, Object>> logs(@PathVariable long id) {
        return service.logs(id);
    }

    @GetMapping("/api/v1/leave-balances")
    public List<Map<String, Object>> balances(@RequestParam(required = false) Long employeeId, @RequestParam(required = false) Integer year) {
        return service.balances(employeeId, year);
    }

    @GetMapping("/api/v1/leave-balances/me")
    public Map<String, Object> myBalance(@RequestParam(required = false) Integer year) {
        return service.myBalance(year);
    }

    @PostMapping("/api/v1/leave-balances/{id}/adjust")
    public Map<String, Object> adjust(@PathVariable long id, @Valid @RequestBody BalanceAdjustRequest r) {
        return service.adjustBalance(id, r.deltaDays(), r.reason());
    }
}
