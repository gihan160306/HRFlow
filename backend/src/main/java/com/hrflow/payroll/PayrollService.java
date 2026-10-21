package com.hrflow.payroll;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.employee.Employee;
import com.hrflow.employee.EmployeeRepository;

@Service
@Transactional
public class PayrollService {

    private final PayrollPeriodRepository periodRepository;
    private final EmployeeRepository employeeRepository;
    private final PayrollCalculator payrollCalculator;

    public PayrollService(
            PayrollPeriodRepository periodRepository,
            EmployeeRepository employeeRepository,
            PayrollCalculator payrollCalculator) {
        this.periodRepository = periodRepository;
        this.employeeRepository = employeeRepository;
        this.payrollCalculator = payrollCalculator;
    }

    @Transactional(readOnly = true)
    public List<PayrollResponse> listPeriods() {
        List<PayrollPeriod> periods = periodRepository.findAllByOrderByStartDateDesc();
        return periods.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PayrollResponse getPeriod(Long periodId) {
        PayrollPeriod period = findPeriod(periodId);
        return toResponse(period);
    }

    public PayrollResponse calculate(Long periodId) {
        PayrollPeriod period = findPeriod(periodId);

        if (period.getStatus() == PayrollStatus.LOCKED) {
            throw BusinessException.conflict("PERIOD_LOCKED", "Kỳ lương đã khóa sổ, không thể tính lại");
        }

        List<Employee> activeEmployees = employeeRepository.findAll().stream()
                .filter(e -> !e.isTerminated())
                .toList();

        List<PayrollResponse.PayrollRecordResponse> records = new ArrayList<>();

        for (Employee emp : activeEmployees) {
            // Chuẩn bị tham số tính lương mẫu (hoặc đọc từ attendance & contract)
            BigDecimal dailySalary = new BigDecimal("1000000"); // 1tr / ngày công
            BigDecimal workDays = new BigDecimal("22");
            BigDecimal allowance = new BigDecimal("3000000");
            BigDecimal otPay = new BigDecimal("1200000");
            BigDecimal bhxh = new BigDecimal("1760000");
            BigDecimal bhyt = new BigDecimal("330000");
            BigDecimal bhtn = new BigDecimal("220000");
            BigDecimal pit = new BigDecimal("1250000");
            BigDecimal otherDeductions = BigDecimal.ZERO;

            PayrollCalculator.SalaryInput input = new PayrollCalculator.SalaryInput(
                    dailySalary,
                    workDays,
                    allowance,
                    otPay,
                    bhxh,
                    bhyt,
                    bhtn,
                    pit,
                    otherDeductions
            );

            PayrollCalculator.SalaryResult result = payrollCalculator.calculate(input);

            records.add(new PayrollResponse.PayrollRecordResponse(
                    emp.getId(),
                    emp.getId(),
                    result.grossSalary(),
                    bhxh,
                    bhyt,
                    bhtn,
                    result.pitAmount(),
                    result.otherDeductions(),
                    result.netSalary()
            ));
        }

        period.setStatus(PayrollStatus.CALCULATED);
        period.setUpdatedAt(Instant.now());
        periodRepository.save(period);

        return new PayrollResponse(
                period.getId(),
                period.getPeriodCode(),
                period.getStartDate(),
                period.getEndDate(),
                period.getStatus(),
                records.size(),
                records
        );
    }

    public PayrollResponse approve(Long periodId, Long userId) {
        PayrollPeriod period = findPeriod(periodId);

        if (period.getStatus() == PayrollStatus.LOCKED) {
            throw BusinessException.conflict("PERIOD_LOCKED", "Kỳ lương đã khóa sổ");
        }

        period.setStatus(PayrollStatus.APPROVED);
        period.setApprovedBy(userId);
        period.setApprovedAt(Instant.now());
        period.setUpdatedAt(Instant.now());
        periodRepository.save(period);

        return toResponse(period);
    }

    public PayrollResponse lock(Long periodId, Long userId) {
        PayrollPeriod period = findPeriod(periodId);

        period.setStatus(PayrollStatus.LOCKED);
        period.setLockedAt(Instant.now());
        period.setUpdatedAt(Instant.now());
        periodRepository.save(period);

        return toResponse(period);
    }

    private PayrollPeriod findPeriod(Long periodId) {
        return periodRepository.findById(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Kỳ tính lương", periodId));
    }

    private PayrollResponse toResponse(PayrollPeriod p) {
        // Sample records summary
        List<PayrollResponse.PayrollRecordResponse> records = List.of(
                new PayrollResponse.PayrollRecordResponse(
                        1L, 1L,
                        new BigDecimal("51200000"),
                        new BigDecimal("3600000"),
                        new BigDecimal("675000"),
                        new BigDecimal("450000"),
                        new BigDecimal("4250000"),
                        BigDecimal.ZERO,
                        new BigDecimal("42225000")
                )
        );

        return new PayrollResponse(
                p.getId(),
                p.getPeriodCode(),
                p.getStartDate(),
                p.getEndDate(),
                p.getStatus(),
                records.size(),
                records
        );
    }
}
