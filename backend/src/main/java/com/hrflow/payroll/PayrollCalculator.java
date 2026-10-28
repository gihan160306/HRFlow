package com.hrflow.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Component;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.payroll.PayrollCalculationRequest.*;

/** Monetary policy effective from July 2026. All final lines use HALF_UP whole VND. */
@Component
public class PayrollCalculator {
    public static final String POLICY_VERSION = "HNQ-2026-07-v1";
    private static final BigDecimal Z = BigDecimal.ZERO;
    private static final BigDecimal BH_CAP = new BigDecimal("50600000");
    private static final BigDecimal BHTN_CAP = new BigDecimal("106200000");

    public record Line(String category, String label, BigDecimal amount, String note) { }
    public record SalaryResult(BigDecimal attendanceSalary, BigDecimal allowance,
            BigDecimal overtimePay, BigDecimal nightExtra, BigDecimal grossSalary, BigDecimal bhxhAmount,
            BigDecimal bhytAmount, BigDecimal bhtnAmount, BigDecimal taxableIncome,
            BigDecimal pitAmount, BigDecimal legalDeductions, BigDecimal netEntitlement,
            BigDecimal netSalary, BigDecimal standardHours, BigDecimal paidHours,
            List<Line> lines) { }

    public SalaryResult calculate(EmployeeInput input) {
        if (input == null || input.sourceReference() == null || input.sourceReference().isBlank())
            invalid("Thiếu nguồn đối soát nhân viên");
        List<Line> lines = new ArrayList<>();
        BigDecimal salaryRaw = Z, standard = Z, paid = Z;
        for (SalarySegment s : required(input.salarySegments(), "salarySegments")) {
            if (s == null || s.sourceReference() == null || s.sourceReference().isBlank()) invalid("Thiếu nguồn đoạn lương");
            nonNegative(s.monthlySalary(), "monthlySalary"); positive(s.standardHours(), "standardHours");
            bounded(s.paidHours(), s.standardHours(), "paidHours");
            salaryRaw = salaryRaw.add(s.monthlySalary().multiply(s.paidHours())
                    .divide(s.standardHours(), 12, RoundingMode.HALF_UP));
            standard = standard.max(s.standardHours()); paid = paid.add(s.paidHours());
        }
        if (input.salarySegments().isEmpty() || paid.compareTo(standard) > 0) invalid("Giờ hưởng lương vượt giờ chuẩn");
        BigDecimal salary = vnd(salaryRaw);
        lines.add(new Line("INCOME", "Lương theo công", salary, POLICY_VERSION));

        BigDecimal allowances = Z, taxableAllowances = Z;
        for (AllowanceInput a : required(input.allowances(), "allowances")) {
            if (a == null || a.code() == null || a.code().isBlank() || a.taxable() == null) invalid("Thiếu phân loại phụ cấp");
            nonNegative(a.monthlyAmount(), "allowance.monthlyAmount"); positive(a.standardHours(), "allowance.standardHours");
            bounded(a.eligibleHours(), a.standardHours(), "allowance.eligibleHours");
            BigDecimal amount = vnd(a.monthlyAmount().multiply(a.eligibleHours())
                    .divide(a.standardHours(), 12, RoundingMode.HALF_UP));
            allowances = allowances.add(amount);
            if (a.taxable()) taxableAllowances = taxableAllowances.add(amount);
            lines.add(new Line("ALLOWANCE", a.code(), amount, "taxable=" + a.taxable()));
        }

        BigDecimal overtime = Z, taxableOvertime = Z;
        for (OvertimeInput o : required(input.overtime(), "overtime")) {
            if (o == null || o.night() == null || o.taxExempt() == null || o.sourceReference() == null
                    || o.sourceReference().isBlank()) invalid("Thiếu xác nhận OT");
            nonNegative(o.hourlyRate(), "ot.hourlyRate"); positive(o.hours(), "ot.hours");
            BigDecimal multiplier = o.dayMultiplier();
            if (multiplier == null || (multiplier.compareTo(new BigDecimal("1.5")) != 0
                    && multiplier.compareTo(new BigDecimal("2")) != 0
                    && multiplier.compareTo(new BigDecimal("3")) != 0)) invalid("Hệ số OT không hợp lệ");
            if (o.night()) {
                BigDecimal dayBase = nonNull(o.nightDayMultiplier(), "ot.nightDayMultiplier");
                boolean validNightDay = multiplier.compareTo(new BigDecimal("1.5")) == 0
                        ? dayBase.compareTo(BigDecimal.ONE) == 0
                            || dayBase.compareTo(new BigDecimal("1.5")) == 0
                        : dayBase.compareTo(multiplier) == 0;
                if (!validNightDay) invalid("Hệ số ban ngày dùng cho OT đêm không hợp lệ");
                multiplier = multiplier.add(new BigDecimal("0.3"))
                        .add(o.nightDayMultiplier().multiply(new BigDecimal("0.2")));
            }
            BigDecimal amount = vnd(o.hourlyRate().multiply(o.hours()).multiply(multiplier));
            overtime = overtime.add(amount);
            if (!o.taxExempt()) taxableOvertime = taxableOvertime.add(amount);
            lines.add(new Line("OVERTIME", "OT " + o.sourceReference(), amount,
                    "night=" + o.night() + ", exempt=" + o.taxExempt()));
        }

        BigDecimal nightExtra = Z, taxableNightExtra = Z;
        for (NightWorkInput n : required(input.nightWork(), "nightWork")) {
            if (n == null || n.taxExempt() == null || n.sourceReference() == null
                    || n.sourceReference().isBlank()) invalid("Thiếu xác nhận giờ làm đêm thường");
            nonNegative(n.hourlyRate(), "nightWork.hourlyRate"); positive(n.hours(), "nightWork.hours");
            BigDecimal amount = vnd(n.hourlyRate().multiply(n.hours()).multiply(new BigDecimal("0.3")));
            nightExtra = nightExtra.add(amount);
            if (!n.taxExempt()) taxableNightExtra = taxableNightExtra.add(amount);
            lines.add(new Line("NIGHT_EXTRA", "Làm đêm " + n.sourceReference(), amount,
                    "exempt=" + n.taxExempt()));
        }

        BigDecimal commission = money(input.commission(), "commission");
        BigDecimal bonus = money(input.bonus(), "bonus");
        BigDecimal otherIncome = money(input.otherTaxableIncome(), "otherTaxableIncome");
        if (commission.signum() > 0) lines.add(new Line("INCOME", "Hoa hồng", commission, input.sourceReference()));
        if (bonus.signum() > 0) lines.add(new Line("INCOME", "Thưởng", bonus, input.sourceReference()));
        if (otherIncome.signum() > 0) lines.add(new Line("INCOME", "Thu nhập khác chịu thuế", otherIncome, input.sourceReference()));
        BigDecimal gross = salary.add(allowances).add(overtime).add(nightExtra)
                .add(commission).add(bonus).add(otherIncome);

        InsuranceInput ins = input.insurance();
        if (ins == null || ins.bhxhParticipating() == null || ins.bhytParticipating() == null
                || ins.bhtnParticipating() == null) invalid("Thiếu xác nhận từng quỹ bảo hiểm");
        BigDecimal bhxh = contribution(ins.bhxhBase(), ins.bhxhParticipating(), BH_CAP, "0.08");
        BigDecimal bhyt = contribution(ins.bhytBase(), ins.bhytParticipating(), BH_CAP, "0.015");
        BigDecimal bhtn = contribution(ins.bhtnBase(), ins.bhtnParticipating(), BHTN_CAP, "0.01");
        lines.add(new Line("INSURANCE", "BHXH", bhxh, POLICY_VERSION));
        lines.add(new Line("INSURANCE", "BHYT", bhyt, POLICY_VERSION));
        lines.add(new Line("INSURANCE", "BHTN", bhtn, POLICY_VERSION));
        BigDecimal insurance = bhxh.add(bhyt).add(bhtn);

        BigDecimal taxable = salary.add(taxableAllowances).add(taxableOvertime).add(taxableNightExtra)
                .add(commission).add(bonus).add(otherIncome);
        TaxInput tax = input.tax();
        if (tax == null || tax.method() == null || tax.validDependents() == null
                || tax.validDependents() < 0 || tax.validShortTermCommitment() == null) invalid("Thiếu phân loại thuế");
        BigDecimal relief = money(tax.otherLegalRelief(), "otherLegalRelief");
        BigDecimal pit;
        if (tax.method() == TaxMethod.PROGRESSIVE) {
            BigDecimal assessable = taxable.subtract(insurance).subtract(new BigDecimal("15500000"))
                    .subtract(new BigDecimal("6200000").multiply(BigDecimal.valueOf(tax.validDependents())))
                    .subtract(relief).max(Z);
            pit = progressive(assessable);
        } else if (tax.method() == TaxMethod.SHORT_TERM) {
            pit = tax.validShortTermCommitment() || taxable.compareTo(new BigDecimal("5000000")) < 0
                    ? Z : vnd(taxable.multiply(new BigDecimal("0.10")));
        } else pit = vnd(taxable.multiply(new BigDecimal("0.20")));
        lines.add(new Line("TAX", "Thuế TNCN", pit, tax.method().name()));

        BigDecimal legal = money(input.legalDeductions(), "legalDeductions");
        BigDecimal advance = money(input.advanceRecovery(), "advanceRecovery");
        BigDecimal reimburse = money(input.reimbursements(), "reimbursements");
        BigDecimal netAdjustment = vnd(nonNull(input.netAdjustment(), "netAdjustment"));
        BigDecimal entitlement = gross.subtract(insurance).subtract(pit).subtract(legal).add(netAdjustment);
        BigDecimal payable = entitlement.subtract(advance).add(reimburse);
        if (payable.signum() < 0) invalid("Thực chi âm; cần quyết định phân bổ khoản phải thu");
        if (legal.signum() > 0) lines.add(new Line("DEDUCTION", "Khấu trừ hợp lệ", legal, input.sourceReference()));
        if (netAdjustment.signum() != 0) lines.add(new Line("ADJUSTMENT", "Điều chỉnh Net", netAdjustment, input.sourceReference()));
        if (advance.signum() > 0) lines.add(new Line("ADVANCE", "Thu hồi tạm ứng", advance, input.sourceReference()));
        if (reimburse.signum() > 0) lines.add(new Line("REIMBURSEMENT", "Hoàn chi", reimburse, input.sourceReference()));
        return new SalaryResult(salary, allowances, overtime, nightExtra, gross, bhxh, bhyt, bhtn,
                taxable, pit, legal, entitlement, payable, standard, paid, List.copyOf(lines));
    }

    private static BigDecimal progressive(BigDecimal amount) {
        BigDecimal[] edges = {Z, new BigDecimal("10000000"), new BigDecimal("30000000"),
                new BigDecimal("60000000"), new BigDecimal("100000000")};
        BigDecimal[] rates = {new BigDecimal("0.05"), new BigDecimal("0.10"),
                new BigDecimal("0.20"), new BigDecimal("0.30"), new BigDecimal("0.35")};
        BigDecimal total = Z;
        for (int i = 0; i < rates.length; i++) {
            BigDecimal upper = i + 1 < edges.length ? amount.min(edges[i + 1]) : amount;
            total = total.add(upper.subtract(edges[i]).max(Z).multiply(rates[i]));
        }
        return vnd(total);
    }
    private static BigDecimal contribution(BigDecimal base, Boolean participating, BigDecimal cap, String rate) {
        nonNegative(base, "insuranceBase");
        if (!participating) {
            if (base.signum() != 0) invalid("Không tham gia bảo hiểm nhưng có căn cứ đóng");
            return Z;
        }
        if (base.signum() == 0) invalid("Tham gia bảo hiểm nhưng thiếu căn cứ đóng");
        return vnd(base.min(cap).multiply(new BigDecimal(rate)));
    }
    private static BigDecimal money(BigDecimal value, String field) { nonNegative(value, field); return vnd(value); }
    private static BigDecimal vnd(BigDecimal v) { return v.setScale(0, RoundingMode.HALF_UP); }
    private static BigDecimal nonNull(BigDecimal v, String field) {
        if (v == null) invalid(field + " không được để trống"); return v;
    }
    private static void nonNegative(BigDecimal v, String field) {
        if (nonNull(v, field).signum() < 0) invalid(field + " không được âm");
    }
    private static void positive(BigDecimal v, String field) {
        if (nonNull(v, field).signum() <= 0) invalid(field + " phải lớn hơn 0");
    }
    private static void bounded(BigDecimal v, BigDecimal max, String field) {
        nonNegative(v, field); if (v.compareTo(max) > 0) invalid(field + " vượt giờ chuẩn");
    }
    private static <T> List<T> required(List<T> v, String field) {
        if (v == null || v.stream().anyMatch(Objects::isNull)) invalid("Thiếu " + field); return v;
    }
    private static void invalid(String message) {
        throw BusinessException.unprocessable("PAYROLL_INPUT_INVALID", message);
    }
}
