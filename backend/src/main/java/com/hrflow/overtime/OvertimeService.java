
package com.hrflow.overtime;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OvertimeService {

    private final OvertimeScheduleRepository scheduleRepository;
    private final OvertimeRegistrationRepository registrationRepository;
    private final JdbcTemplate jdbcTemplate;

    public OvertimeService(
            OvertimeScheduleRepository scheduleRepository,
            OvertimeRegistrationRepository registrationRepository,
            JdbcTemplate jdbcTemplate
    ) {
        this.scheduleRepository = scheduleRepository;
        this.registrationRepository = registrationRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public record ScheduleRequest(
            String title,
            String description,
            LocalDateTime startAt,
            LocalDateTime endAt,
            Integer requiredEmployees
    ) {
    }

    public record DecisionRequest(
            String decision,
            String reason
    ) {
    }

    public record OvertimeHours(
            Long employeeId,
            Long scheduleId,
            LocalDateTime startAt,
            LocalDateTime endAt,
            BigDecimal hours
    ) {
    }

    /**
     * HR/Admin công bố lịch OT.
     */
    @Transactional
    public OvertimeSchedule publishSchedule(
            ScheduleRequest request,
            Long currentUserId,
            String role
    ) {
        requireRole(role, "HR", "ADMIN");

        if (request == null
                || request.title() == null
                || request.title().isBlank()) {
            throw new IllegalArgumentException(
                    "Tên lịch OT không được để trống."
            );
        }

        if (request.startAt() == null
                || request.endAt() == null
                || !request.endAt().isAfter(request.startAt())) {
            throw new IllegalArgumentException(
                    "Thời gian kết thúc phải sau thời gian bắt đầu."
            );
        }

        if (request.requiredEmployees() == null
                || request.requiredEmployees() <= 0) {
            throw new IllegalArgumentException(
                    "Số nhân viên cần OT phải lớn hơn 0."
            );
        }

        OvertimeSchedule schedule = new OvertimeSchedule();
        schedule.setTitle(request.title().trim());
        schedule.setDescription(request.description());
        schedule.setStartAt(request.startAt());
        schedule.setEndAt(request.endAt());
        schedule.setRequiredEmployees(request.requiredEmployees());
        schedule.setCreatedBy(currentUserId);
        schedule.setStatus(
                OvertimeSchedule.ScheduleStatus.PUBLISHED
        );

        return scheduleRepository.save(schedule);
    }

    /**
     * Xem các lịch OT đã công bố.
     */
    @Transactional(readOnly = true)
    public List<OvertimeSchedule> getPublishedSchedules() {
        return scheduleRepository.findByStatusOrderByStartAtAsc(
                OvertimeSchedule.ScheduleStatus.PUBLISHED
        );
    }

    /**
     * Nhân viên đăng ký một lịch OT.
     */
    @Transactional
    public OvertimeRegistration register(
            Long scheduleId,
            Long currentUserId,
            String role
    ) {
        requireRole(role, "EMPLOYEE");

        Long employeeId = getEmployeeId(currentUserId);

        OvertimeSchedule schedule = scheduleRepository
                .findByScheduleIdAndStatus(
                        scheduleId,
                        OvertimeSchedule.ScheduleStatus.PUBLISHED
                )
                .orElseThrow(() -> new IllegalArgumentException(
                        "Lịch OT không tồn tại hoặc không còn mở."
                ));

        if (!schedule.getStartAt().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    "Không thể đăng ký lịch OT đã bắt đầu."
            );
        }

        if (registrationRepository
                .findByScheduleIdAndEmployeeId(
                        scheduleId,
                        employeeId
                )
                .isPresent()) {
            throw new IllegalArgumentException(
                    "Bạn đã đăng ký lịch OT này."
            );
        }

        OvertimeRegistration registration =
                new OvertimeRegistration();

        registration.setScheduleId(schedule.getScheduleId());
        registration.setEmployeeId(employeeId);
        registration.setStatus(OvertimeStatus.PENDING);

        return registrationRepository.save(registration);
    }

    /**
     * Nhân viên xem các đăng ký OT của chính mình.
     */
    @Transactional(readOnly = true)
    public List<OvertimeRegistration> getMyRegistrations(
            Long currentUserId
    ) {
        Long employeeId = getEmployeeId(currentUserId);

        return registrationRepository
                .findByEmployeeIdOrderByCreatedAtDesc(employeeId);
    }

    /**
     * Manager duyệt/từ chối đăng ký của nhân viên trực tiếp.
     * HR được xử lý khi nhân viên không có Manager trực tiếp.
     */
    @Transactional
    public OvertimeRegistration decide(
            Long registrationId,
            DecisionRequest request,
            Long currentUserId,
            String role
    ) {
        if (request == null || request.decision() == null) {
            throw new IllegalArgumentException(
                    "Phải cung cấp quyết định duyệt hoặc từ chối."
            );
        }

        OvertimeRegistration registration =
                registrationRepository.findById(registrationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy đăng ký OT."
                                ));

        if (registration.getStatus() != OvertimeStatus.PENDING) {
            throw new IllegalArgumentException(
                    "Chỉ đăng ký đang chờ duyệt mới được xử lý."
            );
        }

        checkApprovalPermission(
                registration.getEmployeeId(),
                currentUserId,
                role
        );

        String decision = request.decision()
                .trim()
                .toUpperCase();

        if ("APPROVE".equals(decision)) {
            registration.setStatus(OvertimeStatus.APPROVED);
            registration.setDecisionReason(
                    cleanReason(request.reason())
            );
        } else if ("REJECT".equals(decision)) {
            if (request.reason() == null
                    || request.reason().isBlank()) {
                throw new IllegalArgumentException(
                        "Từ chối OT phải có lý do."
                );
            }

            registration.setStatus(OvertimeStatus.REJECTED);
            registration.setDecisionReason(
                    request.reason().trim()
            );
        } else {
            throw new IllegalArgumentException(
                    "decision chỉ nhận APPROVE hoặc REJECT."
            );
        }

        registration.setApprovedBy(currentUserId);
        registration.setDecidedAt(LocalDateTime.now());

        return registrationRepository.save(registration);
    }

    /**
     * Dữ liệu OT đã duyệt trong khoảng thời gian.
     * Payroll có thể gọi service này để lấy giờ OT đủ điều kiện.
     */
    @Transactional(readOnly = true)
    public List<OvertimeHours> getApprovedOvertimeHours(
            LocalDateTime periodStart,
            LocalDateTime periodEnd
    ) {
        if (periodStart == null || periodEnd == null
                || !periodEnd.isAfter(periodStart)) {
            throw new IllegalArgumentException(
                    "Khoảng thời gian không hợp lệ."
            );
        }

        String sql = """
                SELECT
                    r.employee_id,
                    s.schedule_id,
                    s.start_at,
                    s.end_at
                FROM overtime_registration r
                JOIN overtime_schedule s
                    ON s.schedule_id = r.schedule_id
                WHERE r.status = 'APPROVED'
                  AND s.status = 'PUBLISHED'
                  AND s.start_at >= ?
                  AND s.end_at <= ?
                ORDER BY s.start_at
                """;

        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> {
                    LocalDateTime startAt = rs.getTimestamp(
                            "start_at"
                    ).toLocalDateTime();

                    LocalDateTime endAt = rs.getTimestamp(
                            "end_at"
                    ).toLocalDateTime();

                    BigDecimal hours = BigDecimal.valueOf(
                            Duration.between(startAt, endAt)
                                    .toMinutes()
                    ).divide(BigDecimal.valueOf(60), 2,
                            java.math.RoundingMode.HALF_UP);

                    return new OvertimeHours(
                            rs.getLong("employee_id"),
                            rs.getLong("schedule_id"),
                            startAt,
                            endAt,
                            hours
                    );
                },
                java.sql.Timestamp.valueOf(periodStart),
                java.sql.Timestamp.valueOf(periodEnd)
        );
    }

    private Long getEmployeeId(Long userId) {
        List<Long> ids = jdbcTemplate.query(
                """
                SELECT employee_id
                FROM app_user
                WHERE (user_id = ? OR id = ?)
                  AND (active = TRUE OR is_active = TRUE)
                  AND employee_id IS NOT NULL
                """,
                (rs, rowNum) -> rs.getLong("employee_id"),
                userId,
                userId
        );

        if (ids.isEmpty()) {
            throw new AccessDeniedException(
                    "Tài khoản không gắn với hồ sơ nhân viên."
            );
        }

        return ids.get(0);
    }

    private void checkApprovalPermission(
            Long targetEmployeeId,
            Long currentUserId,
            String role
    ) {
        Long managerId = jdbcTemplate.query(
                """
                SELECT manager_id
                FROM employee
                WHERE employee_id = ?
                """,
                rs -> {
                    if (!rs.next()) {
                        return null;
                    }

                    long value = rs.getLong("manager_id");
                    return rs.wasNull() ? null : value;
                },
                targetEmployeeId
        );

        if ("MANAGER".equalsIgnoreCase(role)) {
            Long managerEmployeeId = getEmployeeId(currentUserId);

            if (managerId != null
                    && managerId.equals(managerEmployeeId)) {
                return;
            }

            throw new AccessDeniedException(
                    "Manager chỉ được duyệt nhân viên trực tiếp."
            );
        }

        if ("HR".equalsIgnoreCase(role)) {
            if (managerId == null) {
                return;
            }

            throw new AccessDeniedException(
                    "HR chỉ xử lý đăng ký khi nhân viên không có "
                    + "Manager trực tiếp."
            );
        }

        throw new AccessDeniedException(
                "Bạn không có quyền duyệt đăng ký OT."
        );
    }

    private void requireRole(String actualRole, String... allowedRoles) {
        if (actualRole == null) {
            throw new AccessDeniedException(
                    "Không xác định được vai trò người dùng."
            );
        }

        for (String role : allowedRoles) {
            if (role.equalsIgnoreCase(actualRole)
                    || ("ROLE_" + role).equalsIgnoreCase(actualRole)) {
                return;
            }
        }

        throw new AccessDeniedException(
                "Bạn không có quyền thực hiện thao tác này."
        );
    }

    private String cleanReason(String reason) {
        return reason == null || reason.isBlank()
                ? null
                : reason.trim();
    }
}
