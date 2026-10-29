package com.hrflow.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.payroll.PayrollCalculationRequest.*;

class PayrollCalculatorTest {
    private final PayrollCalculator calculator = new PayrollCalculator();
    private static BigDecimal n(String v) { return new BigDecimal(v); }
    private static SalarySegment salary(String monthly, String standard, String paid) {
        return new SalarySegment("contract-1/attendance-approved", n(monthly), n(standard), n(paid));
    }
    private static InsuranceInput insurance(String base) {
        return new InsuranceInput(n(base), n(base), n(base), true, true, true);
    }
    private static TaxInput progressive() {
        return new TaxInput(TaxMethod.PROGRESSIVE, 0, n("0"), false);
    }
    private static EmployeeInput input(List<SalarySegment> segments, List<AllowanceInput> allowances,
            List<OvertimeInput> ot, InsuranceInput ins) {
        return new EmployeeInput(1L, "hr-verified-source", segments, allowances, ot, List.of(),
                n("0"), n("0"), n("0"), ins, progressive(), n("0"), n("0"), n("0"), n("0"));
    }
    private static List<AllowanceInput> contentAllowances(String hours) {
        return List.of(new AllowanceInput("MEAL", n("730000"), n("176"), n(hours), false),
                new AllowanceInput("PARKING", n("200000"), n("176"), n(hours), true));
    }

    @Test void fullAttendanceAndUnpaidLeaveMatchPolicyExamples() {
        var full = calculator.calculate(input(List.of(salary("10000000", "176", "176")),
                contentAllowances("176"), List.of(), insurance("10000000")));
        assertEquals(n("10930000"), full.grossSalary());
        assertEquals(n("1050000"), full.bhxhAmount().add(full.bhytAmount()).add(full.bhtnAmount()));
        assertEquals(n("9880000"), full.netSalary());

        var unpaid = calculator.calculate(input(List.of(salary("10000000", "176", "160")),
                contentAllowances("160"), List.of(), insurance("10000000")));
        assertEquals(n("9090909"), unpaid.attendanceSalary());
        assertEquals(n("845454"), unpaid.allowance());
        assertEquals(n("9936363"), unpaid.grossSalary());
        assertEquals(n("8886363"), unpaid.netSalary());
    }

    @Test void overtimeTaxAndInsuranceCapsMatchPolicyExamples() {
        var ot = new OvertimeInput("ot-approved-1", n("198863.636363636364"), n("8"),
                n("1.5"), n("1"), false, true);
        var rich = calculator.calculate(input(List.of(salary("35000000", "176", "168")),
                List.of(new AllowanceInput("POSITION", n("1500000"), n("176"), n("176"), true)),
                List.of(ot), insurance("35000000")));
        assertEquals(n("33409091"), rich.attendanceSalary());
        assertEquals(n("2386364"), rich.overtimePay());
        assertEquals(n("37295455"), rich.grossSalary());
        assertEquals(n("1073409"), rich.pitAmount());
        assertEquals(n("32547046"), rich.netSalary());

        var capped = calculator.calculate(input(List.of(salary("60000000", "176", "176")),
                List.of(), List.of(), insurance("60000000")));
        assertEquals(n("4048000"), capped.bhxhAmount());
        assertEquals(n("759000"), capped.bhytAmount());
        assertEquals(n("600000"), capped.bhtnAmount());
    }

    @Test void salaryChangeUsesEachEffectiveRateAndInvalidHoursFail() {
        var changed = calculator.calculate(input(List.of(salary("10000000", "176", "88"),
                salary("12000000", "176", "88")), List.of(), List.of(), insurance("10000000")));
        assertEquals(n("11000000"), changed.attendanceSalary());
        assertThrows(BusinessException.class, () -> calculator.calculate(input(
                List.of(salary("10000000", "176", "177")), List.of(), List.of(), insurance("10000000"))));
    }

    @Test void nightOvertimeUsesPreviousDayMultiplier() {
        var night = new OvertimeInput("ot-night", n("100000"), n("2"), n("1.5"),
                n("1.5"), true, true);
        var result = calculator.calculate(input(List.of(salary("10000000", "176", "176")),
                List.of(), List.of(night), insurance("10000000")));
        assertEquals(n("420000"), result.overtimePay());
        var rest = new OvertimeInput("ot-rest", n("100000"), n("2"), n("2"), n("2"), true, true);
        var holiday = new OvertimeInput("ot-holiday", n("100000"), n("2"), n("3"), n("3"), true, true);
        var weekendAndHoliday = calculator.calculate(input(List.of(salary("10000000", "176", "176")),
                List.of(), List.of(rest, holiday), insurance("10000000")));
        assertEquals(n("1320000"), weekendAndHoliday.overtimePay());
    }

    @Test void shortTermThresholdAndAdvanceAreAppliedOnce() {
        var uninsured = new InsuranceInput(n("0"), n("0"), n("0"), false, false, false);
        var shortTerm = new TaxInput(TaxMethod.SHORT_TERM, 0, n("0"), false);
        var below = new EmployeeInput(1L, "hr-verified-source", List.of(salary("4999999", "176", "176")),
                List.of(), List.of(), List.of(), n("0"), n("0"), n("0"), uninsured, shortTerm,
                n("0"), n("0"), n("0"), n("0"));
        assertEquals(n("0"), calculator.calculate(below).pitAmount());
        var threshold = new EmployeeInput(1L, "hr-verified-source", List.of(salary("5000000", "176", "176")),
                List.of(), List.of(), List.of(), n("0"), n("0"), n("0"), uninsured, shortTerm,
                n("0"), n("1000000"), n("200000"), n("0"));
        var result = calculator.calculate(threshold);
        assertEquals(n("500000"), result.pitAmount());
        assertEquals(n("4500000"), result.netEntitlement());
        assertEquals(n("3700000"), result.netSalary());
    }

    @Test void ordinaryNightAddsOnlyPremiumToAlreadyPaidSalary() {
        var base = input(List.of(salary("10000000", "176", "176")), List.of(), List.of(),
                insurance("10000000"));
        var withNight = new EmployeeInput(base.employeeId(), base.sourceReference(), base.salarySegments(),
                base.allowances(), base.overtime(),
                List.of(new NightWorkInput("night-shift-approved", n("100000"), n("2"), true)),
                base.commission(), base.bonus(), base.otherTaxableIncome(), base.insurance(), base.tax(),
                base.legalDeductions(), base.advanceRecovery(), base.reimbursements(), base.netAdjustment());
        var result = calculator.calculate(withNight);
        assertEquals(n("60000"), result.nightExtra());
        assertEquals(n("10060000"), result.grossSalary());
    }
}
