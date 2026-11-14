package com.hrflow.auth;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
public class AccountController {

    public record CreateAccountRequest(Long employeeId, @NotBlank @Email String email, @NotBlank String role,
                                       String temporaryPassword, @NotBlank String reason, Long requestId) {
    }

    public record ReasonRequest(@NotBlank String reason) {
    }

    public record ChangeRoleRequest(@NotBlank String role, @NotBlank String reason) {
    }

    public record AccountRequestCreate(@NotNull Long employeeId, @NotBlank String requestedRole, String reason) {
    }

    public record ProcessAccountRequest(String email, String temporaryPassword, String note) {
    }

    public record GrantCreateRequest(@NotNull Long accountId, @NotBlank String permission, Long periodId,
                                     Instant validFrom, Instant validTo, @NotBlank String reason,
                                     @NotBlank String decisionRef) {
    }

    public record RouteUpdateRequest(Long primaryApproverId, Long backupApproverId, Long backupDesignatedById,
                                     String backupDesignationRef, @NotNull LocalDate effectiveFrom,
                                     @NotBlank String note) {
    }

    public record UnavailabilityRequest(@NotNull Long employeeId, @NotNull LocalDate fromDate,
                                        @NotNull LocalDate toDate, @NotBlank String reason) {
    }

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/accounts")
    public List<Map<String, Object>> accounts() {
        return service.listAccounts();
    }

    @PostMapping("/api/v1/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody CreateAccountRequest r) {
        return service.createAccount(r.employeeId(), r.email(), r.role(), r.temporaryPassword(), r.reason(), r.requestId());
    }

    @PostMapping("/api/v1/accounts/{id}/lock")
    public Map<String, Object> lock(@PathVariable long id, @Valid @RequestBody ReasonRequest r) {
        return service.lock(id, r.reason());
    }

    @PostMapping("/api/v1/accounts/{id}/unlock")
    public Map<String, Object> unlock(@PathVariable long id, @Valid @RequestBody ReasonRequest r) {
        return service.unlock(id, r.reason());
    }

    @PostMapping("/api/v1/accounts/{id}/reset-password")
    public Map<String, Object> reset(@PathVariable long id, @Valid @RequestBody ReasonRequest r) {
        return service.resetPassword(id, r.reason());
    }

    @PatchMapping("/api/v1/accounts/{id}/role")
    public Map<String, Object> role(@PathVariable long id, @Valid @RequestBody ChangeRoleRequest r) {
        return service.changeRole(id, r.role(), r.reason());
    }

    @GetMapping("/api/v1/account-requests")
    public List<Map<String, Object>> requests() {
        return service.listRequests();
    }

    @PostMapping("/api/v1/account-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createRequest(@Valid @RequestBody AccountRequestCreate r) {
        return service.createRequest(r.employeeId(), r.requestedRole(), r.reason());
    }

    @PostMapping("/api/v1/account-requests/{id}/process")
    public Map<String, Object> process(@PathVariable long id, @RequestBody(required = false) ProcessAccountRequest r) {
        ProcessAccountRequest p = r == null ? new ProcessAccountRequest(null, null, null) : r;
        return service.processRequest(id, p.email(), p.temporaryPassword(), p.note());
    }

    @PostMapping("/api/v1/account-requests/{id}/reject")
    public Map<String, Object> rejectRequest(@PathVariable long id, @Valid @RequestBody ReasonRequest r) {
        return service.rejectRequest(id, r.reason());
    }

    @GetMapping("/api/v1/grants")
    public List<Map<String, Object>> grants() {
        return service.listGrants();
    }

    @PostMapping("/api/v1/grants")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createGrant(@Valid @RequestBody GrantCreateRequest r) {
        return service.createGrant(r.accountId(), r.permission(), r.periodId(), r.validFrom(), r.validTo(), r.reason(),
                r.decisionRef());
    }

    @PostMapping("/api/v1/grants/{id}/revoke")
    public Map<String, Object> revoke(@PathVariable long id, @Valid @RequestBody ReasonRequest r) {
        return service.revokeGrant(id, r.reason());
    }

    @GetMapping("/api/v1/approval-routes")
    public List<Map<String, Object>> routes() {
        return service.listRoutes();
    }

    @PutMapping("/api/v1/approval-routes/{requesterId}")
    public Map<String, Object> updateRoute(@PathVariable long requesterId, @Valid @RequestBody RouteUpdateRequest r) {
        return service.updateRoute(requesterId, r.primaryApproverId(), r.backupApproverId(), r.backupDesignatedById(),
                r.backupDesignationRef(), r.effectiveFrom(), r.note());
    }

    @GetMapping("/api/v1/approver-unavailability")
    public List<Map<String, Object>> unavailability() {
        return service.listUnavailability();
    }

    @PostMapping("/api/v1/approver-unavailability")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createUnavailability(@Valid @RequestBody UnavailabilityRequest r) {
        return service.createUnavailability(r.employeeId(), r.fromDate(), r.toDate(), r.reason());
    }
}
