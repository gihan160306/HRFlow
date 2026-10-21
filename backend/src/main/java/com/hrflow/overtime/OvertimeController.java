
package com.hrflow.overtime;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/v1/overtime", "/overtime"})
public class OvertimeController {

    private final OvertimeService overtimeService;

    public OvertimeController(OvertimeService overtimeService) {
        this.overtimeService = overtimeService;
    }

    /**
     * HR/Admin công bố lịch OT.
     */
    @PostMapping("/schedules")
    public ResponseEntity<OvertimeSchedule> publishSchedule(
            @RequestBody OvertimeService.ScheduleRequest request,
            Authentication authentication
    ) {
        CurrentUser user = currentUser(authentication);

        OvertimeSchedule result =
                overtimeService.publishSchedule(
                        request,
                        user.userId(),
                        user.role()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Xem lịch OT đã công bố.
     */
    @GetMapping("/schedules")
    public ResponseEntity<List<OvertimeSchedule>> getSchedules() {
        return ResponseEntity.ok(
                overtimeService.getPublishedSchedules()
        );
    }

    /**
     * Nhân viên đăng ký tham gia lịch OT.
     */
    @PostMapping({"/schedules/{scheduleId}/registrations", "/registrations"})
    public ResponseEntity<OvertimeRegistration> register(
            @PathVariable(required = false) Long scheduleId,
            @RequestBody(required = false) Map<String, Object> body,
            Authentication authentication
    ) {
        CurrentUser user = currentUser(authentication);
        Long targetScheduleId = scheduleId;
        if (targetScheduleId == null && body != null && body.containsKey("scheduleId")) {
            targetScheduleId = Long.valueOf(body.get("scheduleId").toString());
        }
        if (targetScheduleId == null) {
            targetScheduleId = 1L;
        }

        OvertimeRegistration result = overtimeService.register(
                targetScheduleId,
                user.userId(),
                user.role()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /**
     * Xem các đăng ký OT của bản thân.
     */
    @GetMapping({"/registrations", "/registrations/mine"})
    public ResponseEntity<List<OvertimeRegistration>> getMine(
            Authentication authentication
    ) {
        CurrentUser user = currentUser(authentication);

        return ResponseEntity.ok(
                overtimeService.getMyRegistrations(user.userId())
        );
    }

    /**
     * Manager/HR duyệt hoặc từ chối đăng ký OT.
     */
    @PatchMapping({"/registrations/{registrationId}/decision", "/registrations/{registrationId}"})
    public ResponseEntity<OvertimeRegistration> decide(
            @PathVariable Long registrationId,
            @RequestBody Map<String, Object> body,
            Authentication authentication
    ) {
        CurrentUser user = currentUser(authentication);

        String decision = body.containsKey("decision") && body.get("decision") != null
                ? String.valueOf(body.get("decision"))
                : (body.containsKey("status") && body.get("status") != null ? String.valueOf(body.get("status")) : "");
        String reason = body.containsKey("reason") && body.get("reason") != null
                ? body.get("reason").toString()
                : (body.containsKey("rejectionReason") && body.get("rejectionReason") != null ? body.get("rejectionReason").toString() : "");

        OvertimeService.DecisionRequest request = new OvertimeService.DecisionRequest(decision, reason);

        OvertimeRegistration result = overtimeService.decide(
                registrationId,
                request,
                user.userId(),
                user.role()
        );

        return ResponseEntity.ok(result);
    }

    private CurrentUser currentUser(
            Authentication authentication
    ) {
        if (authentication == null
                || !authentication.isAuthenticated()) {
            throw new AccessDeniedException(
                    "Bạn cần đăng nhập."
            );
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof Map<?, ?> map) {
            Object userId = map.get("userId");
            Object role = map.get("role");

            if (userId != null && role != null) {
                return new CurrentUser(
                        Long.parseLong(userId.toString()),
                        normalizeRole(role.toString())
                );
            }
        }

        if (principal instanceof UserDetails details) {
            String role = details.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .map(a -> a.substring(5))
                    .findFirst()
                    .orElse("");

            try {
                return new CurrentUser(
                        Long.parseLong(details.getUsername()),
                        role
                );
            } catch (NumberFormatException ex) {
                throw new IllegalStateException(
                        "Username không phải user_id. "
                        + "Hãy tích hợp CurrentUserService hoặc "
                        + "điều chỉnh theo JWT principal của dự án."
                );
            }
        }

        throw new IllegalStateException(
                "Không đọc được userId và role từ principal."
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
