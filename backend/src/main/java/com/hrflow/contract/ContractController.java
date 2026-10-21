package com.hrflow.contract;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
@RequestMapping({"/api/v1/contracts", "/contracts"})
@Tag(name = "Contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    /** Phạm vi dữ liệu do service quyết định theo vai trò (record-level). */
    @GetMapping
    public List<ContractResponse> list(@AuthenticationPrincipal AuthUser user,
                                       @RequestParam(required = false) Long employeeId,
                                       @RequestParam(required = false) ContractStatus status) {
        return contractService.list(user, employeeId, status);
    }

    @GetMapping("/{contractId}")
    public ContractResponse get(@AuthenticationPrincipal AuthUser user, @PathVariable Long contractId) {
        return contractService.getById(user, contractId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('HR')")
    public ContractResponse create(@AuthenticationPrincipal AuthUser user,
                                   @Valid @RequestBody ContractRequest request) {
        return contractService.create(user, request);
    }

    @PostMapping("/{contractId}/renew")
    @PreAuthorize("hasRole('HR')")
    public ContractResponse renew(@AuthenticationPrincipal AuthUser user, @PathVariable Long contractId,
                                  @Valid @RequestBody ContractRenewRequest request) {
        return contractService.renew(user, contractId, request);
    }

    @PostMapping("/{contractId}/terminate")
    @PreAuthorize("hasRole('HR')")
    public ContractResponse terminate(@PathVariable Long contractId,
                                      @Valid @RequestBody ContractTerminateRequest request) {
        return contractService.terminate(contractId, request);
    }
}