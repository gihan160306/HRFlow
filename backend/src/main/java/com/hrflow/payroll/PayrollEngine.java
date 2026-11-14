package com.hrflow.payroll;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bộ máy tính lương thuần (không đọc DB, không đọc JSON từ client) theo payroll_rules.md v3.0.
 * Đầu vào là snapshot đã chốt + tham số có hiệu lực; mọi hệ số/trần/giảm trừ đến từ {@link Params}.
 * Số trung gian BigDecimal (chia 12 chữ số thập phân); làm tròn HALF_UP về VND ở từng dòng tiền cuối.
 */
public final class PayrollEngine {

    private static final BigDecimal Z = BigDecimal.ZERO;
    private static final BigDecimal SIXTY = BigDecimal.valueOf(60);
    private static final BigDecimal NIGHT_DAY_FACTOR = new BigDecimal("0.2");
    private static final BigDecimal THIRTY_PERCENT = new BigDecimal("0.30");

    private PayrollEngine() {
    }

    // ---------------- đầu vào ----------------

    public record SalarySeg(LocalDate from, LocalDate to, BigDecimal monthlySalary, long paidMinutes, long stdMinutes,
                            String sourceRef) {
    }

    public record AllowancePart(BigDecimal monthlyAmount, long eligibleMinutes, long stdMinutes) {
    }

    public record AllowanceIn(String code, String name, String taxClass, String status, List<AllowancePart> parts) {
    }

    /** Một đoạn OT đã tách theo 00:00/06:00/22:00 và loại ngày. dayType: WEEKDAY | WEEKLY_OFF | HOLIDAY. */
    public record OtSeg(String sourceRef, String dayType, boolean night, BigDecimal kDay, long minutes,
                        BigDecimal rateBase, long stdMinutes) {
    }

    public record InsuranceIn(boolean bhxh, boolean bhyt, boolean bhtn, BigDecimal insuranceSalary, String status) {
    }

    public record TaxIn(String residency, String method, String status, boolean commitment08, String commitmentDocRef) {
    }

    public record ItemIn(String itemType, String name, BigDecimal amount, String taxClass, String sourceRef) {
    }

    public record AdjIn(long id, String adjType, BigDecimal amount, String taxClass, String sourceRef) {
    }

    public record Input(long employeeId, String employeeCode, String employeeName, String departmentName,
                        String jobTitleName, boolean director, String wageRegion, List<SalarySeg> salary,
                        List<AllowanceIn> allowances, List<OtSeg> ot, boolean salesEligible,
                        BigDecimal salesRevenue, long salesScheduledMinutes, long fullMonthStdMinutes,
                        InsuranceIn insurance, TaxIn tax, int dependents, int unverifiedDependents,
                        List<ItemIn> items, List<AdjIn> adjustments, List<String> dataIssues,
                        List<Map<String, Object>> days) {
    }

    public record Bracket(BigDecimal lower, BigDecimal upper, BigDecimal rate) {
    }

    public record Params(BigDecimal otWeekday, BigDecimal otWeekend, BigDecimal otHoliday, BigDecimal nightPremium,
                         BigDecimal bhxhRate, BigDecimal bhytRate, BigDecimal bhtnRate, BigDecimal refSalary,
                         BigDecimal bhCapMultiplier, BigDecimal bhtnCapMultiplier, BigDecimal minWage,
                         BigDecimal personalDeduction, BigDecimal dependentDeduction, BigDecimal flat10Threshold,
                         BigDecimal flat10Rate, BigDecimal nonResidentRate, BigDecimal commissionRate,
                         BigDecimal salesTarget, BigDecimal kpiBonus, List<Bracket> brackets, Set<String> unverified) {
    }

    // ---------------- đầu ra ----------------

    public record Item(int seq, String category, String code, String name, BigDecimal amountRaw, BigDecimal amount,
                       String sourceRef, String rule) {
    }

    public record Result(String status, List<String> issues, BigDecimal hStd, BigDecimal hPaid, BigDecimal baseSalary,
                         BigDecimal salaryAttendance, BigDecimal otPay, BigDecimal allowances, BigDecimal commission,
                         BigDecimal kpiBonus, BigDecimal bonuses, BigDecimal otherAdditions, BigDecimal grossAdjustment,
                         BigDecimal gross, BigDecimal nonTaxable, BigDecimal exempt, BigDecimal taxable, BigDecimal bhxh,
                         BigDecimal bhyt, BigDecimal bhtn, BigDecimal insuranceTotal, BigDecimal familyRelief,
                         BigDecimal assessable, BigDecimal pit, BigDecimal legalDeductions, BigDecimal netAdjustment,
                         BigDecimal netEntitlement, BigDecimal advanceRecovery, BigDecimal reimbursements,
                         BigDecimal payable, List<Item> items) {
    }

    // ---------------- tính ----------------

    private static final class Acc {
        final List<Item> items = new ArrayList<>();
        final List<String> blocking = new ArrayList<>();
        final List<String> warnings = new ArrayList<>();
        BigDecimal nonTaxable = Z;
        BigDecimal exempt = Z;

        Item add(String category, String code, String name, BigDecimal raw, BigDecimal amount, String ref, String rule) {
            Item i = new Item(items.size() + 1, category, code, name, raw, amount, ref, rule);
            items.add(i);
            return i;
        }

        void classify(String taxClass, BigDecimal amount, String what) {
            switch (taxClass == null ? "NEEDS_REVIEW" : taxClass) {
                case "NON_TAXABLE" -> nonTaxable = nonTaxable.add(amount);
                case "EXEMPT" -> exempt = exempt.add(amount);
                case "TAXABLE" -> {
                }
                default -> blocking.add(what + ": chưa phân loại thuế (NEEDS_REVIEW) - HR xác nhận trước khi duyệt; tạm tính là chịu thuế");
            }
        }
    }

    public static Result calculate(Input in, Params p) {
        Acc a = new Acc();
        for (String issue : in.dataIssues()) {
            (issue.startsWith("Lưu ý") ? a.warnings : a.blocking).add(issue);
        }
        for (String code : p.unverified()) {
            a.blocking.add("Tham số " + code + " chưa được xác nhận (NEEDS_REVIEW)");
        }

        // --- Lương theo công: Σ L_s × H_paid_s / H_std_s, làm tròn một lần ---
        BigDecimal salaryRaw = Z;
        long paidMin = 0;
        long stdMax = 0;
        BigDecimal baseSalary = Z;
        for (SalarySeg s : in.salary()) {
            if (s.stdMinutes() <= 0) {
                if (s.paidMinutes() > 0 || s.monthlySalary().signum() > 0) {
                    a.blocking.add("H_std = 0 theo lịch ở đoạn " + s.from() + ": HR phải xác định chế độ lương áp dụng");
                }
                continue;
            }
            salaryRaw = salaryRaw.add(s.monthlySalary().multiply(BigDecimal.valueOf(s.paidMinutes()))
                    .divide(BigDecimal.valueOf(s.stdMinutes()), 12, RoundingMode.HALF_UP));
            paidMin += s.paidMinutes();
            stdMax = Math.max(stdMax, s.stdMinutes());
            baseSalary = s.monthlySalary();
        }
        BigDecimal salary = vnd(salaryRaw);
        a.add("INCOME", "SALARY_ATTENDANCE", "Lương theo công", salaryRaw, salary, null, "Σ L × H_paid / H_std");

        // --- Phụ cấp: Σ A × H_eligible / H_std theo từng đoạn, làm tròn một lần cho mỗi khoản ---
        BigDecimal allowanceTotal = Z;
        for (AllowanceIn al : in.allowances()) {
            BigDecimal raw = Z;
            for (AllowancePart part : al.parts()) {
                if (part.stdMinutes() > 0) {
                    raw = raw.add(part.monthlyAmount().multiply(BigDecimal.valueOf(part.eligibleMinutes()))
                            .divide(BigDecimal.valueOf(part.stdMinutes()), 12, RoundingMode.HALF_UP));
                }
            }
            BigDecimal amount = vnd(raw);
            if (amount.signum() == 0) {
                continue;
            }
            allowanceTotal = allowanceTotal.add(amount);
            a.add("ALLOWANCE", al.code(), al.name(), raw, amount, null, "Σ A × H_eligible / H_std; thuế=" + al.taxClass());
            a.classify(al.taxClass(), amount, "Phụ cấp " + al.code());
            if ("NEEDS_REVIEW".equals(al.status())) {
                a.blocking.add("Phụ cấp " + al.code() + " chưa được xác nhận điều kiện hưởng");
            }
        }

        // --- OT: gộp đoạn chưa làm tròn theo dòng (loại ngày, đêm) rồi ROUND từng dòng ---
        Map<String, BigDecimal> otRaw = new LinkedHashMap<>();
        for (OtSeg o : in.ot()) {
            if (o.stdMinutes() <= 0) {
                a.blocking.add("Không xác định được đơn giá OT (H_std = 0) cho " + o.sourceRef());
                continue;
            }
            BigDecimal k = switch (o.dayType()) {
                case "HOLIDAY" -> p.otHoliday();
                case "WEEKLY_OFF" -> p.otWeekend();
                default -> p.otWeekday();
            };
            BigDecimal mult = o.night() ? k.add(p.nightPremium()).add(NIGHT_DAY_FACTOR.multiply(o.kDay())) : k;
            // R_ot × giờ × hệ số = L_base/H_std × (phút/60) × hệ số = L_base × phút × hệ số / std (một phép chia ở cuối)
            BigDecimal raw = o.rateBase().multiply(BigDecimal.valueOf(o.minutes())).multiply(mult)
                    .divide(BigDecimal.valueOf(o.stdMinutes()), 12, RoundingMode.HALF_UP);
            otRaw.merge(o.dayType() + (o.night() ? "_NIGHT" : ""), raw, BigDecimal::add);
        }
        BigDecimal otPay = Z;
        for (Map.Entry<String, BigDecimal> e : otRaw.entrySet()) {
            BigDecimal amount = vnd(e.getValue());
            otPay = otPay.add(amount);
            a.add("OVERTIME", "OT_" + e.getKey(), "OT " + e.getKey(), e.getValue(), amount, null,
                    "R_ot × giờ thực tế × hệ số (miễn thuế trong điều kiện hợp pháp bộ 2026)");
        }
        a.exempt = a.exempt.add(otPay);

        // --- Hoa hồng và KPI (chỉ Sales Executive được giao sở hữu doanh thu) ---
        BigDecimal commission = Z;
        BigDecimal kpi = Z;
        if (in.salesEligible()) {
            BigDecimal rev = in.salesRevenue();
            BigDecimal commRaw = rev.multiply(p.commissionRate());
            commission = vnd(commRaw);
            if (commission.signum() > 0) {
                a.add("INCOME", "COMMISSION", "Hoa hồng Sales", commRaw, commission, null, "Doanh thu đủ điều kiện × tỷ lệ");
            }
            BigDecimal target = in.fullMonthStdMinutes() <= 0 ? Z : p.salesTarget().multiply(BigDecimal.valueOf(in.salesScheduledMinutes()))
                    .divide(BigDecimal.valueOf(in.fullMonthStdMinutes()), 12, RoundingMode.HALF_UP);
            if (target.signum() > 0 && rev.compareTo(target) >= 0) {
                kpi = vnd(p.kpiBonus());
                a.add("INCOME", "KPI_BONUS", "Thưởng KPI", p.kpiBonus(), kpi, null, "Doanh thu ≥ chỉ tiêu theo lịch giữ vị trí Sales");
            }
        }

        // --- Khoản thu nhập khác, khấu trừ, tạm ứng, hoàn chi ---
        BigDecimal bonuses = Z, other = Z, legalRaw = Z, compensation = Z, advance = Z, reimb = Z;
        for (ItemIn i : in.items()) {
            BigDecimal amt = vnd(i.amount());
            switch (i.itemType()) {
                case "BONUS" -> {
                    bonuses = bonuses.add(amt);
                    a.add("INCOME", "BONUS", i.name(), i.amount(), amt, i.sourceRef(), "Thưởng được xác nhận");
                    a.classify(i.taxClass(), amt, "Thưởng " + i.sourceRef());
                }
                case "OTHER_ADDITION" -> {
                    other = other.add(amt);
                    a.add("INCOME", "OTHER_ADDITION", i.name(), i.amount(), amt, i.sourceRef(), "Khoản bổ sung hợp lệ");
                    a.classify(i.taxClass(), amt, "Khoản bổ sung " + i.sourceRef());
                }
                case "DEDUCTION" -> legalRaw = legalRaw.add(amt);
                case "COMPENSATION" -> compensation = compensation.add(amt);
                case "ADVANCE_RECOVERY" -> advance = advance.add(amt);
                case "REIMBURSEMENT" -> reimb = reimb.add(amt);
                default -> a.blocking.add("Loại khoản không hỗ trợ: " + i.itemType());
            }
        }

        // --- Điều chỉnh sau khóa kỳ: chỉ GROSS đi qua thuế; TAX/INSURANCE/NET là điều chỉnh tiền thuần ---
        BigDecimal grossAdj = Z, netAdj = Z;
        for (AdjIn j : in.adjustments()) {
            BigDecimal amt = vnd(j.amount());
            if ("GROSS".equals(j.adjType())) {
                grossAdj = grossAdj.add(amt);
                a.add("ADJUSTMENT", "ADJ_GROSS", "Điều chỉnh Gross " + j.sourceRef(), j.amount(), amt, j.sourceRef(), "Qua phân loại thuế");
                a.classify(j.taxClass(), amt, "Điều chỉnh " + j.sourceRef());
            } else {
                netAdj = netAdj.add(amt);
                a.add("ADJUSTMENT", "ADJ_" + j.adjType(), "Điều chỉnh " + j.adjType() + " " + j.sourceRef(), j.amount(), amt, j.sourceRef(),
                        "Điều chỉnh tiền thuần (không thay tờ khai thuế/bảo hiểm kỳ gốc)");
            }
        }

        BigDecimal gross = salary.add(allowanceTotal).add(otPay).add(commission).add(kpi).add(bonuses).add(other).add(grossAdj);

        // --- Bảo hiểm: căn cứ riêng đã xác nhận, trần theo tham số ---
        BigDecimal bhxh = Z, bhyt = Z, bhtn = Z;
        InsuranceIn ins = in.insurance();
        if (ins == null) {
            a.blocking.add("Thiếu hồ sơ bảo hiểm (đối tượng từng quỹ, mức đóng)");
        } else {
            if (!"CONFIRMED".equals(ins.status())) {
                a.blocking.add("Hồ sơ bảo hiểm chưa được HR xác nhận (NEEDS_REVIEW)");
            }
            BigDecimal capBh = p.refSalary().multiply(p.bhCapMultiplier());
            BigDecimal capTn = p.minWage().multiply(p.bhtnCapMultiplier());
            if (ins.bhxh()) {
                bhxh = vnd(ins.insuranceSalary().min(capBh).multiply(p.bhxhRate()));
            }
            if (ins.bhyt()) {
                bhyt = vnd(ins.insuranceSalary().min(capBh).multiply(p.bhytRate()));
            }
            if (ins.bhtn()) {
                bhtn = vnd(ins.insuranceSalary().min(capTn).multiply(p.bhtnRate()));
            }
        }
        BigDecimal insuranceTotal = bhxh.add(bhyt).add(bhtn);
        a.add("INSURANCE", "BHXH", "BHXH người lao động", bhxh, bhxh, null, "min(mức đóng, trần) × tỷ lệ");
        a.add("INSURANCE", "BHYT", "BHYT người lao động", bhyt, bhyt, null, "min(mức đóng, trần) × tỷ lệ");
        a.add("INSURANCE", "BHTN", "BHTN người lao động", bhtn, bhtn, null, "min(mức đóng, trần vùng) × tỷ lệ");

        // --- Thuế ---
        BigDecimal taxable = gross.subtract(a.exempt).subtract(a.nonTaxable).max(Z);
        BigDecimal relief = p.personalDeduction().add(p.dependentDeduction().multiply(BigDecimal.valueOf(in.dependents())));
        BigDecimal assessable = Z;
        BigDecimal pit = Z;
        TaxIn tax = in.tax();
        if (tax == null) {
            a.blocking.add("Thiếu hồ sơ thuế (cư trú, phương pháp khấu trừ)");
        } else {
            if (!"CONFIRMED".equals(tax.status())) {
                a.blocking.add("Hồ sơ thuế chưa được HR xác nhận (NEEDS_REVIEW)");
            }
            switch (tax.method()) {
                case "PROGRESSIVE" -> {
                    assessable = taxable.subtract(insuranceTotal).subtract(relief).max(Z);
                    pit = progressive(assessable, p.brackets());
                }
                case "FLAT_10" -> {
                    boolean exemptByCommitment = tax.commitment08() && tax.commitmentDocRef() != null && !tax.commitmentDocRef().isBlank();
                    pit = exemptByCommitment || taxable.compareTo(p.flat10Threshold()) < 0 ? Z : vnd(taxable.multiply(p.flat10Rate()));
                }
                case "NON_RESIDENT_20" -> pit = vnd(taxable.multiply(p.nonResidentRate()));
                default -> a.blocking.add("Phương pháp khấu trừ " + tax.method() + " cần HR xác nhận kết quả có chứng từ; engine không tự tính");
            }
        }
        if (in.unverifiedDependents() > 0) {
            a.warnings.add("Có " + in.unverifiedDependents() + " người phụ thuộc chưa xác minh nên chưa được giảm trừ");
        }
        a.add("TAX", "PIT", "Thuế TNCN", pit, pit, null, tax == null ? "-" : tax.method());

        // --- Khấu trừ hợp lệ (bồi thường tối đa 30% thực trả sau BH và thuế) ---
        BigDecimal afterTax = gross.subtract(insuranceTotal).subtract(pit);
        BigDecimal cap30 = vnd(afterTax.max(Z).multiply(THIRTY_PERCENT));
        BigDecimal compDeducted = compensation.min(cap30);
        if (compensation.compareTo(compDeducted) > 0) {
            a.warnings.add("Khoản bồi thường khấu trừ kỳ này giới hạn 30%; còn lại " + compensation.subtract(compDeducted).toPlainString() + " VND chuyển thu kỳ sau");
        }
        BigDecimal legal = legalRaw.add(compDeducted);
        if (legalRaw.signum() > 0) {
            a.add("DEDUCTION", "LEGAL_DEDUCTION", "Khấu trừ có căn cứ", legalRaw, legalRaw, null, "Có chứng từ và quyết định");
        }
        if (compDeducted.signum() > 0) {
            a.add("DEDUCTION", "COMPENSATION", "Bồi thường khấu trừ (≤30%)", compensation, compDeducted, null, "Điều 102 BLLĐ");
        }
        BigDecimal netEntitlement = gross.subtract(insuranceTotal).subtract(pit).subtract(legal).add(netAdj);
        if (advance.signum() > 0) {
            a.add("ADVANCE", "ADVANCE_RECOVERY", "Thu hồi tạm ứng", advance, advance, null, "Chỉ ghi một lần");
        }
        if (reimb.signum() > 0) {
            a.add("REIMBURSEMENT", "REIMBURSEMENT", "Hoàn chi (ngoài Gross)", reimb, reimb, null, "Không cộng Gross");
        }
        BigDecimal payable = netEntitlement.subtract(advance).add(reimb);
        if (payable.signum() < 0) {
            a.blocking.add("Thực chi âm (" + payable.toPlainString() + "): cần quyết định phân bổ khoản phải thu, không tự ép về 0");
        }

        List<String> issues = new ArrayList<>(a.blocking);
        issues.addAll(a.warnings);
        return new Result(a.blocking.isEmpty() ? "OK" : "NEEDS_REVIEW", issues,
                BigDecimal.valueOf(stdMax).divide(SIXTY, 4, RoundingMode.HALF_UP),
                BigDecimal.valueOf(paidMin).divide(SIXTY, 4, RoundingMode.HALF_UP), baseSalary, salary, otPay,
                allowanceTotal, commission, kpi, bonuses, other, grossAdj, gross, a.nonTaxable, a.exempt, taxable, bhxh,
                bhyt, bhtn, insuranceTotal, relief, assessable, pit, legal, netAdj, netEntitlement, advance, reimb,
                payable, a.items);
    }

    /** PIT = Σ max(0, min(T, upper) − lower) × rate, làm tròn một lần sau khi cộng các bậc. */
    static BigDecimal progressive(BigDecimal t, List<Bracket> brackets) {
        BigDecimal total = Z;
        for (Bracket b : brackets) {
            BigDecimal top = b.upper() == null ? t : t.min(b.upper());
            total = total.add(top.subtract(b.lower()).max(Z).multiply(b.rate()));
        }
        return vnd(total);
    }

    static BigDecimal vnd(BigDecimal x) {
        return x.setScale(0, RoundingMode.HALF_UP);
    }
}
