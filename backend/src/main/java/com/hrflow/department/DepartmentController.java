package com.hrflow.department;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping({"/api/v1/departments", "/departments"})
@Tag(name = "Departments")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /** Mọi người dùng đã đăng nhập đều xem được (dùng cho dropdown); activeOnly=true để chỉ lấy phòng ban đang hoạt động. */
    @GetMapping
    public List<DepartmentResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return departmentService.list(activeOnly);
    }

    @GetMapping("/{departmentId}")
    public DepartmentResponse get(@PathVariable Long departmentId) {
        return departmentService.get(departmentId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public DepartmentResponse create(@Valid @RequestBody DepartmentCreateRequest request) {
        return departmentService.create(request);
    }

    @PatchMapping("/{departmentId}")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public DepartmentResponse update(@PathVariable Long departmentId,
                                     @Valid @RequestBody DepartmentUpdateRequest request) {
        return departmentService.update(departmentId, request);
    }
}