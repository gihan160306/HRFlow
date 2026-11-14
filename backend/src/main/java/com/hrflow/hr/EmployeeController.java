package com.hrflow.hr;

import java.math.BigDecimal;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.common.response.PageResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@RestController
public class EmployeeController {

    public record EmployeeCreateRequest(@NotBlank String code, @NotBlank String fullName, String gender,
                                        LocalDate dateOfBirth, String phone, @Email String personalEmail,
                                        @NotBlank @Email String workEmail, String address, String nationalId,
                                        String bankName, String bankAccountNo, @NotBlank String employeeType,
                                        @NotNull LocalDate hireDate, String workLocation, String wageRegion,
                                        @NotNull Long departmentId, @NotNull Long jobTitleId, Long managerId,
                                        Long scheduleId) {
    }

    public record EmployeeUpdateRequest(String fullName, String gender, LocalDate dateOfBirth, String phone,
                                        String personalEmail, String address, String nationalId, String bankName,
                                        String bankAccountNo, String employeeType, String workLocation,
                                        String wageRegion, @NotBlank String reason, Long version) {
    }

    public record SelfUpdateRequest(String phone, @Email String personalEmail, String address) {
    }

    public record TerminateRequest(@NotNull LocalDate terminationDate, @NotBlank String reason) {
    }

    public record SalaryCreateRequest(@NotNull BigDecimal baseSalary, @NotNull LocalDate effectiveFrom,
                                      @NotBlank String reason, String decisionRef) {
    }

    public record AllowanceRequest(@NotNull Long allowanceTypeId, @NotNull BigDecimal amount,
                                   @NotNull LocalDate effectiveFrom, LocalDate effectiveTo,
                                   @NotBlank String decisionRef) {
    }

    public record EndRequest(@NotNull LocalDate effectiveTo, @NotBlank String reason) {
    }

    public record AssignmentRequest(@NotNull Long departmentId, @NotNull Long jobTitleId, Long managerId,
                                    @NotNull LocalDate effectiveFrom, @NotBlank String reason, String decisionRef) {
    }

    public record DependentRequest(@NotBlank String fullName, @NotBlank String relationship,
                                   @NotNull LocalDate validFrom, LocalDate validTo, String docRef, Boolean verified) {
    }

    public record InsuranceRequest(@NotNull LocalDate effectiveFrom, @NotNull Boolean bhxh, @NotNull Boolean bhyt,
                                   @NotNull Boolean bhtn, @NotNull BigDecimal insuranceSalary,
                                   @NotBlank String status, String note) {
    }

    public record TaxProfileRequest(@NotNull LocalDate effectiveFrom, @NotBlank String residency,
                                    @NotBlank String method, String taxCode, Boolean commitment08,
                                    String commitmentDocRef, @NotBlank String status, String note) {
    }

    public record ScheduleRequest(@NotNull Long scheduleId, @NotNull LocalDate effectiveFrom, @NotBlank String reason) {
    }

    private final EmployeeService service;
    private final EmployeeRecordsService records;

    public EmployeeController(EmployeeService service, EmployeeRecordsService records) {
        this.service = service;
        this.records = records;
    }

    @GetMapping("/api/v1/employees")
    public PageResponse<Map<String, Object>> list(@RequestParam(defaultValue = "0") int page,
                                                  @RequestParam(defaultValue = "20") int size,
                                                  @RequestParam(required = false) String q,
                                                  @RequestParam(required = false) Long departmentId,
                                                  @RequestParam(required = false) String status) {
        return service.list(Math.max(page, 0), Math.min(Math.max(size, 1), 200), q, departmentId, status);
    }

    @GetMapping("/api/v1/employees/options")
    public List<Map<String, Object>> options() {
        return service.options();
    }

    @PostMapping("/api/v1/employees")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody EmployeeCreateRequest r) {
        return service.create(new EmployeeService.CreateCmd(r.code(), r.fullName(), r.gender(), r.dateOfBirth(),
                r.phone(), r.personalEmail(), r.workEmail(), r.address(), r.nationalId(), r.bankName(),
                r.bankAccountNo(), r.employeeType(), r.hireDate(), r.workLocation(), r.wageRegion(),
                r.departmentId(), r.jobTitleId(), r.managerId(), r.scheduleId()));
    }

    @GetMapping("/api/v1/employees/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        return service.detail(id);
    }

    @PatchMapping("/api/v1/employees/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody EmployeeUpdateRequest r) {
        return service.update(id, new EmployeeService.UpdateCmd(r.fullName(), r.gender(), r.dateOfBirth(), r.phone(),
                r.personalEmail(), r.address(), r.nationalId(), r.bankName(), r.bankAccountNo(), r.employeeType(),
                r.workLocation(), r.wageRegion(), r.reason(), r.version()));
    }

    @GetMapping("/api/v1/employees/{id}/onboarding-check")
    public Map<String, Object> onboardingCheck(@PathVariable long id) {
        return service.onboardingCheck(id);
    }

    @PostMapping("/api/v1/employees/{id}/activate")
    public Map<String, Object> activate(@PathVariable long id) {
        return service.activate(id);
    }

    @PostMapping("/api/v1/employees/{id}/terminate")
    public Map<String, Object> terminate(@PathVariable long id, @Valid @RequestBody TerminateRequest r) {
        return service.terminate(id, r.terminationDate(), r.reason());
    }

    @GetMapping("/api/v1/me/profile")
    public Map<String, Object> myProfile() {
        return service.myProfile();
    }

    @PatchMapping("/api/v1/me/profile")
    public Map<String, Object> updateMyProfile(@Valid @RequestBody SelfUpdateRequest r) {
        return service.updateMyProfile(r.phone(), r.personalEmail(), r.address());
    }

    // ---- hồ sơ có hiệu lực ----

    @GetMapping("/api/v1/employees/{id}/salaries")
    public List<Map<String, Object>> salaries(@PathVariable long id) {
        return records.salaries(id);
    }

    @PostMapping("/api/v1/employees/{id}/salaries")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addSalary(@PathVariable long id, @Valid @RequestBody SalaryCreateRequest r) {
        return records.addSalary(id, r.baseSalary(), r.effectiveFrom(), r.reason(), r.decisionRef());
    }

    @GetMapping("/api/v1/employees/{id}/allowances")
    public List<Map<String, Object>> allowances(@PathVariable long id) {
        return records.allowances(id);
    }

    @PostMapping("/api/v1/employees/{id}/allowances")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addAllowance(@PathVariable long id, @Valid @RequestBody AllowanceRequest r) {
        return records.addAllowance(id, r.allowanceTypeId(), r.amount(), r.effectiveFrom(), r.effectiveTo(), r.decisionRef());
    }

    @PostMapping("/api/v1/employee-allowances/{id}/end")
    public Map<String, Object> endAllowance(@PathVariable long id, @Valid @RequestBody EndRequest r) {
        return records.endAllowance(id, r.effectiveTo(), r.reason());
    }

    @GetMapping("/api/v1/employees/{id}/assignments")
    public List<Map<String, Object>> assignments(@PathVariable long id) {
        return records.assignments(id);
    }

    @PostMapping("/api/v1/employees/{id}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addAssignment(@PathVariable long id, @Valid @RequestBody AssignmentRequest r) {
        return records.addAssignment(id, r.departmentId(), r.jobTitleId(), r.managerId(), r.effectiveFrom(), r.reason(), r.decisionRef());
    }

    @GetMapping("/api/v1/employees/{id}/dependents")
    public List<Map<String, Object>> dependents(@PathVariable long id) {
        return records.dependents(id);
    }

    @PostMapping("/api/v1/employees/{id}/dependents")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addDependent(@PathVariable long id, @Valid @RequestBody DependentRequest r) {
        return records.addDependent(id, r.fullName(), r.relationship(), r.validFrom(), r.validTo(), r.docRef(), r.verified());
    }

    @PutMapping("/api/v1/dependents/{id}")
    public Map<String, Object> updateDependent(@PathVariable long id, @Valid @RequestBody DependentRequest r) {
        return records.updateDependent(id, r.fullName(), r.relationship(), r.validFrom(), r.validTo(), r.docRef(), r.verified());
    }

    @GetMapping("/api/v1/employees/{id}/insurance-profiles")
    public List<Map<String, Object>> insurance(@PathVariable long id) {
        return records.insuranceProfiles(id);
    }

    @PostMapping("/api/v1/employees/{id}/insurance-profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addInsurance(@PathVariable long id, @Valid @RequestBody InsuranceRequest r) {
        return records.addInsurance(id, r.effectiveFrom(), r.bhxh(), r.bhyt(), r.bhtn(), r.insuranceSalary(), r.status(), r.note());
    }

    @GetMapping("/api/v1/employees/{id}/tax-profiles")
    public List<Map<String, Object>> taxProfiles(@PathVariable long id) {
        return records.taxProfiles(id);
    }

    @PostMapping("/api/v1/employees/{id}/tax-profiles")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addTaxProfile(@PathVariable long id, @Valid @RequestBody TaxProfileRequest r) {
        return records.addTaxProfile(id, r.effectiveFrom(), r.residency(), r.method(), r.taxCode(), r.commitment08(),
                r.commitmentDocRef(), r.status(), r.note());
    }

    @GetMapping("/api/v1/employees/{id}/schedules")
    public List<Map<String, Object>> schedules(@PathVariable long id) {
        return records.schedules(id);
    }

    @PostMapping("/api/v1/employees/{id}/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> addSchedule(@PathVariable long id, @Valid @RequestBody ScheduleRequest r) {
        return records.addSchedule(id, r.scheduleId(), r.effectiveFrom(), r.reason());
    }
}
