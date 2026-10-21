
package com.hrflow.payslip;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PayslipController {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    /**
     * GET /api/v1/employees/{employeeId}/payslips
     * Xem danh sách phiếu lương của nhân viên.
     */
    @GetMapping("/employees/{employeeId}/payslips")
    public ResponseEntity<List<PayslipResponse>> getEmployeePayslips(
            @PathVariable Long employeeId,
            Authentication authentication
    ) {
        CurrentUser currentUser = getCurrentUser(authentication);

        List<PayslipResponse> result =
                payslipService.getEmployeePayslips(
                        employeeId,
                        currentUser.userId(),
                        currentUser.role()
                );

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/payslips/{payslipId}
     * Xem chi tiết một phiếu lương.
     */
    @GetMapping("/payslips/{payslipId}")
    public ResponseEntity<PayslipResponse> getPayslip(
            @PathVariable Long payslipId,
            Authentication authentication
    ) {
        CurrentUser currentUser = getCurrentUser(authentication);

        PayslipResponse result = payslipService.getPayslip(
                payslipId,
                currentUser.userId(),
                currentUser.role()
        );

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/v1/payslips/me
     * Xem phiếu lương của người dùng hiện tại
     */
    @GetMapping({"/payslips/me", "/payslips"})
    public ResponseEntity<List<PayslipResponse>> getMyPayslips(
            Authentication authentication
    ) {
        CurrentUser currentUser = getCurrentUser(authentication);
        List<PayslipResponse> result = payslipService.getEmployeePayslips(
                currentUser.userId(),
                currentUser.userId(),
                currentUser.role()
        );
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy userId và role từ principal do Spring Security cung cấp.
     */
    private CurrentUser getCurrentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getPrincipal() == null) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Bạn cần đăng nhập để thực hiện thao tác này."
            );
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof com.hrflow.security.AuthUser authUser) {
            return new CurrentUser(
                    authUser.userId() != null ? authUser.userId() : 1L,
                    normalizeRole(authUser.role() != null ? authUser.role() : "EMPLOYEE")
            );
        }

        if (principal instanceof Map<?, ?> userInfo) {
            Object userId = userInfo.get("userId");
            Object role = userInfo.get("role");

            if (userId != null && role != null) {
                return new CurrentUser(
                        Long.valueOf(userId.toString()),
                        normalizeRole(role.toString())
                );
            }
        }

        // Trường hợp principal là UserDetails.
        // Chỉ dùng nếu username của tài khoản là user_id.
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            String username = userDetails.getUsername();

            try {
                return new CurrentUser(
                        Long.parseLong(username),
                        userDetails.getAuthorities().stream()
                                .map(authority -> authority.getAuthority())
                                .filter(authority ->
                                        authority.startsWith("ROLE_"))
                                .map(authority ->
                                        authority.substring(5))
                                .findFirst()
                                .orElse("")
                );
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(
                        "Principal hiện tại không cung cấp userId. "
                        + "Hãy kết nối với CurrentUserService của dự án."
                );
            }
        }

        throw new IllegalStateException(
                "Chưa xác định được userId và role từ JWT principal. "
                + "Hãy điều chỉnh getCurrentUser() theo cấu hình Security "
                + "của HRFlow."
        );
    }

    private String normalizeRole(String role) {
        return role.startsWith("ROLE_")
                ? role.substring(5)
                : role;
    }

    private record CurrentUser(Long userId, String role) {
    }
}
