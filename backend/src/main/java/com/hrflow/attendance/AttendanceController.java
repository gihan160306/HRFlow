package com.hrflow.attendance;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@RestController
public class AttendanceController {

    public record ScanRequest(String qrPayload, String shortCode, String photoBase64, String photoMime) {
    }

    public record NoteBody(String note) {
    }

    public record AdjustmentRequest(Long employeeId, @NotNull LocalDate workDate, Instant proposedCheckIn,
                                    Instant proposedCheckOut, @NotBlank String method, @NotBlank String reason,
                                    String evidence) {
    }

    public record AbsenceRequest(@NotNull Long employeeId, @NotEmpty List<LocalDate> dates, @NotBlank String reason) {
    }

    public record PointRequest(@NotBlank String code, @NotBlank String name, Boolean photoRequired, Boolean active) {
    }

    private final AttendanceService service;

    public AttendanceController(AttendanceService service) {
        this.service = service;
    }

    @PostMapping("/api/v1/attendance/check-in")
    public Map<String, Object> checkIn(@RequestBody(required = false) ScanRequest r) {
        return service.checkIn(r == null ? null : new AttendanceService.Scan(r.qrPayload(), r.shortCode(), r.photoBase64(), r.photoMime()));
    }

    @PostMapping("/api/v1/attendance/check-out")
    public Map<String, Object> checkOut(@RequestBody(required = false) ScanRequest r) {
        return service.checkOut(r == null ? null : new AttendanceService.Scan(r.qrPayload(), r.shortCode(), r.photoBase64(), r.photoMime()));
    }

    @GetMapping("/api/v1/attendance/today")
    public ResponseEntity<Map<String, Object>> today() {
        Map<String, Object> rec = service.today();
        return rec == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(rec);
    }

    @GetMapping("/api/v1/attendance")
    public List<Map<String, Object>> list(@RequestParam(required = false) Long employeeId,
                                          @RequestParam(required = false) LocalDate from,
                                          @RequestParam(required = false) LocalDate to,
                                          @RequestParam(required = false) String status) {
        return service.list(employeeId, from, to, status);
    }

    @GetMapping("/api/v1/attendance/review")
    public List<Map<String, Object>> review(@RequestParam int year, @RequestParam int month) {
        return service.review(year, month);
    }

    @GetMapping("/api/v1/attendance/timesheet")
    public Map<String, Object> timesheet(@RequestParam(required = false) Long employeeId, @RequestParam int year,
                                         @RequestParam int month) {
        return service.timesheet(employeeId, year, month);
    }

    @PostMapping("/api/v1/attendance/{id}/confirm")
    public Map<String, Object> confirm(@PathVariable long id, @RequestBody(required = false) NoteBody r) {
        return service.confirm(id, r == null ? null : r.note());
    }

    @GetMapping("/api/v1/attendance/adjustments")
    public List<Map<String, Object>> adjustments(@RequestParam(required = false) String status) {
        return service.adjustments(status);
    }

    @PostMapping("/api/v1/attendance/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAdjustment(@Valid @RequestBody AdjustmentRequest r) {
        return service.createAdjustment(new AttendanceService.AdjustmentCmd(r.employeeId(), r.workDate(), r.proposedCheckIn(),
                r.proposedCheckOut(), r.method(), r.reason(), r.evidence()));
    }

    @PostMapping("/api/v1/attendance/adjustments/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id, @RequestBody(required = false) NoteBody r) {
        return service.decideAdjustment(id, true, r == null ? null : r.note());
    }

    @PostMapping("/api/v1/attendance/adjustments/{id}/reject")
    public Map<String, Object> reject(@PathVariable long id, @RequestBody(required = false) NoteBody r) {
        return service.decideAdjustment(id, false, r == null ? null : r.note());
    }

    @PostMapping("/api/v1/attendance/absences")
    @ResponseStatus(HttpStatus.CREATED)
    public List<Map<String, Object>> absences(@Valid @RequestBody AbsenceRequest r) {
        return service.markAbsences(r.employeeId(), r.dates(), r.reason());
    }

    @GetMapping("/api/v1/attendance-points")
    public List<Map<String, Object>> points() {
        return service.points();
    }

    @PostMapping("/api/v1/attendance-points")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPoint(@Valid @RequestBody PointRequest r) {
        return service.savePoint(null, r.code(), r.name(), r.photoRequired(), r.active());
    }

    @PutMapping("/api/v1/attendance-points/{id}")
    public Map<String, Object> updatePoint(@PathVariable long id, @Valid @RequestBody PointRequest r) {
        return service.savePoint(id, r.code(), r.name(), r.photoRequired(), r.active());
    }

    @GetMapping("/api/v1/attendance/qr")
    public Map<String, Object> qr(@RequestParam long pointId) {
        return service.issueQr(pointId);
    }

    @GetMapping("/api/v1/attendance/{id}/photo")
    public ResponseEntity<byte[]> photo(@PathVariable long id, @RequestParam(defaultValue = "in") String kind) {
        Map<String, Object> p = service.photo(id, kind);
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.parseMediaType((String) p.get("mime_type")).toString())
                .body((byte[]) p.get("content"));
    }
}
