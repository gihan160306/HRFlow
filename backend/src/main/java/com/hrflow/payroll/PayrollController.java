package com.hrflow.payroll;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.security.AuthUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/v1/payroll", "/payroll"})
@Tag(name = "Payroll")
public class PayrollController {

    private final PayrollService payrollService;

    public PayrollController(PayrollService payrollService) {
        this.payrollService = payrollService;
    }

    @GetMapping("/periods")
    @PreAuthorize("@payrollAccess.has(authentication, 'VIEW')")
    public ResponseEntity<List<PayrollResponse>> listPeriods() {
        return ResponseEntity.ok(payrollService.listPeriods());
    }

    @GetMapping("/periods/{id}")
    @PreAuthorize("@payrollAccess.has(authentication, 'VIEW')")
    public ResponseEntity<PayrollResponse> getPeriod(@PathVariable Long id) {
        return ResponseEntity.ok(payrollService.getPeriod(id));
    }

    @PostMapping("/calculate")
    @PreAuthorize("@payrollAccess.has(authentication, 'PREPARE')")
    public ResponseEntity<PayrollResponse> calculatePayroll(
            @Valid @RequestBody PayrollCalculationRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(payrollService.calculate(request, user.userId()));
    }

    @PostMapping("/periods/{id}/approve")
    @PreAuthorize("@payrollAccess.has(authentication, 'APPROVE')")
    public ResponseEntity<PayrollResponse> approvePayroll(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        Long userId = user != null ? user.userId() : null;
        return ResponseEntity.ok(payrollService.approve(id, userId));
    }

    @PostMapping("/periods/{id}/reset")
    @PreAuthorize("@payrollAccess.has(authentication, 'PREPARE')")
    public ResponseEntity<PayrollResponse> resetPayroll(
            @PathVariable Long id, @Valid @RequestBody PayrollResetRequest request,
            @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(payrollService.reset(id, user.userId(), request.reason()));
    }

    @PostMapping("/periods/{id}/lock")
    @PreAuthorize("@payrollAccess.has(authentication, 'LOCK')")
    public ResponseEntity<PayrollResponse> lockPayroll(
            @PathVariable Long id,
            @AuthenticationPrincipal AuthUser user) {
        Long userId = user != null ? user.userId() : null;
        return ResponseEntity.ok(payrollService.lock(id, userId));
    }

    @PostMapping("/periods/{id}/publish")
    @PreAuthorize("@payrollAccess.has(authentication, 'LOCK')")
    public ResponseEntity<PayrollResponse> publishPayslips(
            @PathVariable Long id, @AuthenticationPrincipal AuthUser user) {
        return ResponseEntity.ok(payrollService.publish(id, user.userId()));
    }
}
