
package com.hrflow.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

@Component
public class PayrollCalculator {

    /**
     * Dữ liệu đầu vào đã được hệ thống lấy và xác thực.
     *
     * dailySalary: tiền lương một ngày công theo quy tắc dự án.
     * actualWorkingDays: số ngày công thực tế được xác nhận.
     * allowance: tổng phụ cấp hợp lệ.
     * overtimePay: tiền OT đã được tính theo quy tắc OT của dự án.
     */
    public record SalaryInput(
            BigDecimal dailySalary,
            BigDecimal actualWorkingDays,
            BigDecimal allowance,
            BigDecimal overtimePay,
            BigDecimal bhxhAmount,
            BigDecimal bhytAmount,
            BigDecimal bhtnAmount,
            BigDecimal pitAmount,
            BigDecimal otherDeductions
    ) {
    }

    public record SalaryResult(
            BigDecimal attendanceSalary,
            BigDecimal allowance,
            BigDecimal overtimePay,
            BigDecimal grossSalary,
            BigDecimal totalInsurance,
            BigDecimal pitAmount,
            BigDecimal otherDeductions,
            BigDecimal netSalary
    ) {
    }

    public SalaryResult calculate(SalaryInput input) {
        validate(input);

        // Lương theo ngày công thực tế.
        BigDecimal attendanceSalary = input.dailySalary()
                .multiply(input.actualWorkingDays())
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal allowance = input.allowance();
        BigDecimal overtimePay = input.overtimePay();

        // Gross Salary = Lương ngày công + Phụ cấp + Tiền OT.
        BigDecimal grossSalary = attendanceSalary
                .add(allowance)
                .add(overtimePay)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalInsurance = input.bhxhAmount()
                .add(input.bhytAmount())
                .add(input.bhtnAmount());

        // Không tự đặt tỷ lệ thuế hoặc bảo hiểm.
        // Các khoản này phải được xác định từ quy tắc đã duyệt.
        BigDecimal totalDeductions = totalInsurance
                .add(input.pitAmount())
                .add(input.otherDeductions());

        BigDecimal netSalary = grossSalary
                .subtract(totalDeductions)
                .setScale(2, RoundingMode.HALF_UP);

        return new SalaryResult(
                attendanceSalary,
                allowance,
                overtimePay,
                grossSalary,
                totalInsurance,
                input.pitAmount(),
                input.otherDeductions(),
                netSalary
        );
    }

    private void validate(SalaryInput input) {
        if (input == null) {
            throw new IllegalArgumentException(
                    "Dữ liệu tính lương không được để trống."
            );
        }

        requireNonNegative(input.dailySalary(), "dailySalary");
        requireNonNegative(input.actualWorkingDays(), "actualWorkingDays");
        requireNonNegative(input.allowance(), "allowance");
        requireNonNegative(input.overtimePay(), "overtimePay");
        requireNonNegative(input.bhxhAmount(), "bhxhAmount");
        requireNonNegative(input.bhytAmount(), "bhytAmount");
        requireNonNegative(input.bhtnAmount(), "bhtnAmount");
        requireNonNegative(input.pitAmount(), "pitAmount");
        requireNonNegative(input.otherDeductions(), "otherDeductions");
    }

    private void requireNonNegative(
            BigDecimal value,
            String fieldName
    ) {
        if (value == null || value.signum() < 0) {
            throw new IllegalArgumentException(
                    fieldName + " không được null hoặc âm."
            );
        }
    }
}
