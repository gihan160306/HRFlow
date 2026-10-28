package com.hrflow.payslip;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.hrflow.security.AuthUser;

@RestController
@RequestMapping("/api/v1")
public class PayslipController {
    private final PayslipService payslips;
    public PayslipController(PayslipService payslips) { this.payslips = payslips; }

    @GetMapping("/employees/{employeeId}/payslips")
    public ResponseEntity<List<PayslipResponse>> getEmployeePayslips(
            @PathVariable Long employeeId, @AuthenticationPrincipal AuthUser user) {
        requireUser(user);
        return ResponseEntity.ok(payslips.getEmployeePayslips(employeeId, user.userId(), user.role()));
    }

    @GetMapping("/payslips/{payslipId}")
    public ResponseEntity<PayslipResponse> getPayslip(
            @PathVariable Long payslipId, @AuthenticationPrincipal AuthUser user) {
        requireUser(user);
        return ResponseEntity.ok(payslips.getPayslip(payslipId, user.userId(), user.role()));
    }

    @GetMapping({"/payslips/me", "/payslips"})
    public ResponseEntity<List<PayslipResponse>> getMyPayslips(@AuthenticationPrincipal AuthUser user) {
        requireUser(user);
        if (user.employeeId() == null) throw new AccessDeniedException("Tài khoản chưa gắn hồ sơ nhân viên");
        return ResponseEntity.ok(payslips.getMyPublishedPayslips(user.employeeId(), user.userId(), user.role()));
    }

    private static void requireUser(AuthUser user) {
        if (user == null || !user.active()) throw new AccessDeniedException("Bạn cần đăng nhập");
    }
}
