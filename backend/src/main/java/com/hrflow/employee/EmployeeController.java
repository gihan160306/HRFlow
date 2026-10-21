package com.hrflow.employee;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.common.response.PageResponse;
import com.hrflow.security.AuthUser;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping({"/api/v1/employees", "/employees"})
@Validated
@Tag(name = "Employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    /** HR/Admin: toàn bộ. Manager: chỉ team trực tiếp. */
    @GetMapping
    @PreAuthorize("hasAnyRole('HR','ADMIN','MANAGER')")
    public PageResponse<EmployeeResponse> list(
            @AuthenticationPrincipal AuthUser user,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Employee.Status status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return employeeService.search(user, departmentId, status,
                PageRequest.of(page, size, Sort.by("employeeCode")));
    }

    /** Employee xem chính mình; Manager xem team; HR/Admin xem tất cả (kiểm tra record-level ở service). */
    @GetMapping("/{employeeId}")
    public EmployeeResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long employeeId) {
        return employeeService.getById(user, employeeId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PatchMapping("/{employeeId}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public EmployeeResponse update(@PathVariable Long employeeId,
                                   @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(employeeId, request);
    }
}