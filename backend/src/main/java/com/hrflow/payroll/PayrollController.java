package com.hrflow.payroll;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Đường dẫn khớp docs/openapi-spec.yaml (không còn /payroll/calculate nhận JSON từ client). */
@RestController
public class PayrollController {

    public record PeriodCreateRequest(@NotNull Integer year, @NotNull Integer month, LocalDate payDate) {
    }

    public record ReasonBody(@NotBlank String reason) {
    }

    public record NoteBody(String note) {
    }

    public record PaymentRequest(@NotBlank String paymentRef, LocalDate paidDate) {
    }

    public record ConflictResolutionRequest(@NotBlank String decisionRef, @NotBlank String authorityConfirmedBy,
                                            @NotNull LocalDate authorityConfirmedAt, @NotBlank String note) {
    }

    public record AdjustmentRequest(@NotNull Long originPeriodId, @NotNull Long employeeId, @NotBlank String adjType,
                                    @NotNull BigDecimal amount, String taxClass, @NotBlank String reason,
                                    @NotBlank String evidence, @NotBlank String sourceRef) {
    }

    public record ParameterRequest(@NotBlank String code, @NotBlank String name, @NotBlank String category,
                                   @NotNull BigDecimal value, @NotBlank String unit, String region,
                                   @NotNull LocalDate effectiveFrom, @NotBlank String sourceRef,
                                   @NotBlank String status, String confirmedBy) {
    }

    private final PayrollService service;
    private final PayslipService payslips;

    public PayrollController(PayrollService service, PayslipService payslips) {
        this.service = service;
        this.payslips = payslips;
    }

    @GetMapping("/api/v1/payroll/periods")
    public List<Map<String, Object>> periods() {
        return service.listPeriods();
    }

    @PostMapping("/api/v1/payroll/periods")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createPeriod(@Valid @RequestBody PeriodCreateRequest r) {
        return service.createPeriod(r.year(), r.month(), r.payDate());
    }

    @GetMapping("/api/v1/payroll/periods/{id}")
    public Map<String, Object> period(@PathVariable long id) {
        return service.getPeriod(id);
    }

    @GetMapping("/api/v1/payroll/periods/{id}/precheck")
    public Map<String, Object> precheck(@PathVariable long id) {
        return service.precheck(id);
    }

    @PostMapping("/api/v1/payroll/periods/{id}/close-timesheet")
    public Map<String, Object> closeTimesheet(@PathVariable long id) {
        return service.closeTimesheet(id);
    }

    @PostMapping("/api/v1/payroll/periods/{id}/reopen-timesheet")
    public Map<String, Object> reopenTimesheet(@PathVariable long id, @Valid @RequestBody ReasonBody r) {
        return service.reopenTimesheet(id, r.reason());
    }

    @PostMapping("/api/v1/payroll/periods/{id}/calculate")
    public Map<String, Object> calculate(@PathVariable long id) {
        return service.calculate(id);
    }

    @PostMapping("/api/v1/payroll/periods/{id}/reopen")
    public Map<String, Object> reopen(@PathVariable long id, @Valid @RequestBody ReasonBody r) {
        return service.reopen(id, r.reason());
    }

    @PostMapping("/api/v1/payroll/periods/{id}/reconcile")
    public Map<String, Object> reconcile(@PathVariable long id, @RequestBody(required = false) NoteBody r) {
        return service.reconcile(id, r == null ? null : r.note());
    }

    @PostMapping("/api/v1/payroll/periods/{id}/conflict-resolution")
    public Map<String, Object> conflict(@PathVariable long id, @Valid @RequestBody ConflictResolutionRequest r) {
        return service.conflictResolution(id, r.decisionRef(), r.authorityConfirmedBy(), r.authorityConfirmedAt(), r.note());
    }

    @PostMapping("/api/v1/payroll/periods/{id}/approve")
    public Map<String, Object> approve(@PathVariable long id) {
        return service.approve(id);
    }

    @PostMapping("/api/v1/payroll/periods/{id}/lock")
    public Map<String, Object> lock(@PathVariable long id) {
        return service.lock(id);
    }

    @PostMapping("/api/v1/payroll/periods/{id}/mark-paid")
    public Map<String, Object> markPaid(@PathVariable long id, @Valid @RequestBody PaymentRequest r) {
        return service.markPaid(id, r.paymentRef(), r.paidDate());
    }

    @GetMapping("/api/v1/payroll/periods/{id}/lines")
    public List<Map<String, Object>> lines(@PathVariable long id) {
        return service.lines(id);
    }

    @GetMapping("/api/v1/payroll/lines/{lineId}")
    public Map<String, Object> line(@PathVariable long lineId) {
        return service.line(lineId);
    }

    @GetMapping("/api/v1/payroll/periods/{id}/audit")
    public List<Map<String, Object>> audit(@PathVariable long id) {
        return service.audit(id);
    }

    @GetMapping("/api/v1/payroll/periods/{id}/export")
    public ResponseEntity<byte[]> export(@PathVariable long id) {
        byte[] csv = service.exportCsv(id).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok().contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("bang-luong-" + id + ".csv").build().toString())
                .body(csv);
    }

    @GetMapping("/api/v1/payroll/periods/{id}/payslips")
    public List<Map<String, Object>> periodPayslips(@PathVariable long id) {
        return payslips.ofPeriod(id);
    }

    @GetMapping("/api/v1/payroll/adjustments")
    public List<Map<String, Object>> adjustments() {
        return service.adjustments();
    }

    @PostMapping("/api/v1/payroll/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAdjustment(@Valid @RequestBody AdjustmentRequest r) {
        return service.createAdjustment(new PayrollService.AdjCmd(r.originPeriodId(), r.employeeId(), r.adjType(), r.amount(),
                r.taxClass(), r.reason(), r.evidence(), r.sourceRef()));
    }

    @PostMapping("/api/v1/payroll/adjustments/{id}/approve")
    public Map<String, Object> approveAdjustment(@PathVariable long id, @RequestBody(required = false) NoteBody r) {
        return service.decideAdjustment(id, true, r == null ? null : r.note());
    }

    @PostMapping("/api/v1/payroll/adjustments/{id}/reject")
    public Map<String, Object> rejectAdjustment(@PathVariable long id, @RequestBody(required = false) NoteBody r) {
        return service.decideAdjustment(id, false, r == null ? null : r.note());
    }

    @GetMapping("/api/v1/parameters")
    public List<Map<String, Object>> parameters(@RequestParam(required = false) String category) {
        return service.parameters(category);
    }

    @PostMapping("/api/v1/parameters")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createParameter(@Valid @RequestBody ParameterRequest r) {
        return service.createParameter(new PayrollService.ParamCmd(r.code(), r.name(), r.category(), r.value(), r.unit(), r.region(),
                r.effectiveFrom(), r.sourceRef(), r.status(), r.confirmedBy()));
    }

    @GetMapping("/api/v1/payslips/me")
    public List<Map<String, Object>> myPayslips() {
        return payslips.mine();
    }

    @GetMapping("/api/v1/payslips/{id}")
    public Map<String, Object> payslip(@PathVariable long id) {
        return payslips.get(id);
    }

    @GetMapping("/api/v1/payslips/{id}/pdf")
    public ResponseEntity<byte[]> payslipPdf(@PathVariable long id, @RequestParam(defaultValue = "false") boolean download) {
        Map<String, Object> row = payslips.pdf(id, download);
        ContentDisposition cd = (download ? ContentDisposition.attachment() : ContentDisposition.inline()).filename((String) row.get("file_name")).build();
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .body((byte[]) row.get("pdf"));
    }
}
