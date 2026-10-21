package com.hrflow.attendance;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.security.AuthUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/v1/attendance", "/attendance"})
@Tag(name = "Attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /** Người dùng chỉ chấm công cho chính mình; Admin không chấm công (permission_matrix). */
    @PostMapping("/check-in")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR')")
    public AttendanceResponse checkIn(@AuthenticationPrincipal AuthUser user) {
        return attendanceService.checkIn(user);
    }

    @PostMapping("/check-out")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MANAGER','HR')")
    public AttendanceResponse checkOut(@AuthenticationPrincipal AuthUser user) {
        return attendanceService.checkOut(user);
    }

    @GetMapping
    public List<AttendanceResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return attendanceService.list(user, employeeId, from, to);
    }

    /** HR điều chỉnh bản ghi công; bắt buộc có lý do. */
    @PatchMapping
    @PreAuthorize("hasRole('HR')")
    public AttendanceResponse adjust(@AuthenticationPrincipal AuthUser user,
                                     @Valid @RequestBody AttendanceAdjustRequest request) {
        return attendanceService.adjust(user, request);
    }
}