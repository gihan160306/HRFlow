package com.hrflow.payroll;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.hrflow.payroll.PayrollEngine.AllowanceIn;
import com.hrflow.payroll.PayrollEngine.AllowancePart;
import com.hrflow.payroll.PayrollEngine.Bracket;
import com.hrflow.payroll.PayrollEngine.InsuranceIn;
import com.hrflow.payroll.PayrollEngine.Input;
import com.hrflow.payroll.PayrollEngine.OtSeg;
import com.hrflow.payroll.PayrollEngine.Params;
import com.hrflow.payroll.PayrollEngine.Result;
import com.hrflow.payroll.PayrollEngine.SalarySeg;
import com.hrflow.payroll.PayrollEngine.TaxIn;

/** Ví dụ đối soát P01–P08 và ca biên B06, B08, B09, B11–B14, B22 của payroll_rules.md Mục 11. */
class PayrollEngineTest {

    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    private static final Params P = new Params(d("1.5"), d("2"), d("3"), d("0.3"), d("0.08"), d("0.015"), d("0.01"),
            d("2530000"), d("20"), d("20"), d("5310000"), d("15500000"), d("6200000"), d("5000000"), d("0.10"),
            d("0.20"), d("0.02"), d("100000000"), d("1500000"),
            List.of(new Bracket(d("0"), d("10000000"), d("0.05")), new Bracket(d("10000000"), d("30000000"), d("0.10")),
                    new Bracket(d("30000000"), d("60000000"), d("0.20")), new Bracket(d("60000000"), d("100000000"), d("0.30")),
                    new Bracket(d("100000000"), null, d("0.35"))), Set.of());

    private static final LocalDate A = LocalDate.of(2026, 11, 1);
    private static final LocalDate B = LocalDate.of(2026, 11, 30);

    private static InsuranceIn ins(String salary) {
        return new InsuranceIn(true, true, true, d(salary), "CONFIRMED");
    }

    private static final TaxIn PROG = new TaxIn("RESIDENT", "PROGRESSIVE", "CONFIRMED", false, null);

    private static AllowanceIn allowance(String code, String taxClass, String amount, long elig, long std) {
        return new AllowanceIn(code, code, taxClass, "CONFIRMED", List.of(new AllowancePart(d(amount), elig, std)));
    }

    private static Input input(List<SalarySeg> salary, List<AllowanceIn> allowances, List<OtSeg> ot, InsuranceIn ins) {
        return new Input(1, "NV", "Test", "Dept", "Title", false, "REGION_I", salary, allowances, ot, false, BigDecimal.ZERO, 0, 0,
                ins, PROG, 0, 0, List.of(), List.of(), List.of(), List.of());
    }

    private static SalarySeg seg(String salary, long paidH, long stdH) {
        return new SalarySeg(A, B, d(salary), paidH * 60, stdH * 60, "t");
    }

    @Test
    void p01FullMonth() {
        Result r = PayrollEngine.calculate(input(List.of(seg("10000000", 176, 176)),
                List.of(allowance("LUNCH", "NON_TAXABLE", "730000", 176 * 60, 176 * 60), allowance("PARKING", "TAXABLE", "200000", 176 * 60, 176 * 60)),
                List.of(), ins("10000000")), P);
        assertEquals(d("10930000"), r.gross());
        assertEquals(d("1050000"), r.insuranceTotal());
        assertEquals(d("0"), r.pit());
        assertEquals(d("9880000"), r.payable());
        assertEquals("OK", r.status());
    }

    @Test
    void p02TwoUnpaidDays() {
        Result r = PayrollEngine.calculate(input(List.of(seg("10000000", 160, 176)),
                List.of(allowance("LUNCH", "NON_TAXABLE", "730000", 160 * 60, 176 * 60), allowance("PARKING", "TAXABLE", "200000", 160 * 60, 176 * 60)),
                List.of(), ins("10000000")), P);
        assertEquals(d("9090909"), r.salaryAttendance());
        assertEquals(d("9936363"), r.gross());
        assertEquals(d("8886363"), r.payable());
    }

    @Test
    void p03MidMonthJoiner() {
        Result r = PayrollEngine.calculate(input(List.of(seg("10000000", 88, 176)),
                List.of(allowance("LUNCH", "NON_TAXABLE", "730000", 88 * 60, 176 * 60), allowance("PARKING", "TAXABLE", "200000", 88 * 60, 176 * 60)),
                List.of(), ins("10000000")), P);
        assertEquals(d("5000000"), r.salaryAttendance());
        assertEquals(d("5465000"), r.gross());
        assertEquals(d("4415000"), r.payable());
    }

    @Test
    void p04PartTimeHalfMonth() {
        Result r = PayrollEngine.calculate(input(List.of(seg("5000000", 40, 80)),
                List.of(allowance("LUNCH_PT", "NON_TAXABLE", "365000", 40 * 60, 80 * 60), allowance("PARKING_PT", "TAXABLE", "100000", 40 * 60, 80 * 60)),
                List.of(), ins("5000000")), P);
        assertEquals(d("2500000"), r.salaryAttendance());
        assertEquals(d("2732500"), r.gross());
        assertEquals(d("525000"), r.insuranceTotal());
        assertEquals(d("2207500"), r.payable());
    }

    @Test
    void p05SalesKpi() {
        Input base = input(List.of(seg("9000000", 176, 176)),
                List.of(allowance("LUNCH", "NON_TAXABLE", "730000", 176 * 60, 176 * 60), allowance("PHONE", "NON_TAXABLE", "300000", 176 * 60, 176 * 60),
                        allowance("PARKING", "TAXABLE", "200000", 176 * 60, 176 * 60)),
                List.of(), ins("9000000"));
        Input in = new Input(base.employeeId(), "NV", "S", "Sales", "Sales Exec", false, "REGION_I", base.salary(), base.allowances(), List.of(),
                true, d("100000000"), 176 * 60, 176 * 60, base.insurance(), PROG, 0, 0, List.of(), List.of(), List.of(), List.of());
        Result r = PayrollEngine.calculate(in, P);
        assertEquals(d("2000000"), r.commission());
        assertEquals(d("1500000"), r.kpiBonus());
        assertEquals(d("13730000"), r.gross());
        assertEquals(d("945000"), r.insuranceTotal());
        assertEquals(d("12785000"), r.payable());
    }

    @Test
    void p06OvertimeAndTax() {
        OtSeg ot = new OtSeg("OT", "WEEKDAY", false, d("1"), 8 * 60, d("35000000"), 176 * 60);
        Result r = PayrollEngine.calculate(input(List.of(seg("35000000", 168, 176)),
                List.of(allowance("POS", "TAXABLE", "1500000", 176 * 60, 176 * 60)), List.of(ot), ins("35000000")), P);
        assertEquals(d("33409091"), r.salaryAttendance());
        assertEquals(d("2386364"), r.otPay());
        assertEquals(d("37295455"), r.gross());
        assertEquals(d("3675000"), r.insuranceTotal());
        assertEquals(d("34909091"), r.taxable());
        assertEquals(d("15734091"), r.assessable());
        assertEquals(d("1073409"), r.pit());
        assertEquals(d("32547046"), r.payable());
    }

    @Test
    void p07InsuranceCaps() {
        Result r = PayrollEngine.calculate(input(List.of(seg("60000000", 176, 176)), List.of(), List.of(), ins("60000000")), P);
        assertEquals(d("4048000"), r.bhxh());
        assertEquals(d("759000"), r.bhyt());
        assertEquals(d("600000"), r.bhtn());
        assertEquals(d("5407000"), r.insuranceTotal());
    }

    @Test
    void p08SalaryChangeMidMonth() {
        Result r = PayrollEngine.calculate(input(List.of(seg("10000000", 88, 176), seg("12000000", 88, 176)), List.of(), List.of(), ins("10000000")), P);
        assertEquals(d("11000000"), r.salaryAttendance());
    }

    @Test
    void b06AllowanceAllocationRoundsOncePerAllowance() {
        Result r = PayrollEngine.calculate(input(List.of(seg("10000000", 72, 176)),
                List.of(allowance("LUNCH", "NON_TAXABLE", "730000", 72 * 60, 176 * 60), allowance("PARKING", "TAXABLE", "200000", 72 * 60, 176 * 60)),
                List.of(), ins("10000000")), P);
        assertEquals(d("298636"), r.items().stream().filter(i -> i.code().equals("LUNCH")).findFirst().orElseThrow().amount());
        assertEquals(d("81818"), r.items().stream().filter(i -> i.code().equals("PARKING")).findFirst().orElseThrow().amount());
    }

    @Test
    void b08b09OvertimeMultipliers() {
        // R_ot = 100.000/giờ => lương cơ sở 100.000 × 176 giờ; 2 giờ OT
        BigDecimal base = d("17600000");
        long std = 176 * 60;
        assertEquals(d("300000"), otPay("WEEKDAY", false, "1", base, std));
        assertEquals(d("400000"), otPay("WEEKLY_OFF", false, "2", base, std));
        assertEquals(d("600000"), otPay("HOLIDAY", false, "3", base, std));
        assertEquals(d("400000"), otPay("WEEKDAY", true, "1", base, std));
        assertEquals(d("420000"), otPay("WEEKDAY", true, "1.5", base, std));
        assertEquals(d("540000"), otPay("WEEKLY_OFF", true, "2", base, std));
        assertEquals(d("780000"), otPay("HOLIDAY", true, "3", base, std));
    }

    private static BigDecimal otPay(String dayType, boolean night, String kDay, BigDecimal base, long std) {
        OtSeg ot = new OtSeg("OT", dayType, night, d(kDay), 120, base, std);
        return PayrollEngine.calculate(input(List.of(seg("17600000", 176, 176)), List.of(), List.of(ot), ins("17600000")), P).otPay();
    }

    @Test
    void b11b12Kpi() {
        for (Object[] c : new Object[][] {{"80000000", "1600000", "0"}, {"100000000", "2000000", "1500000"}, {"150000000", "3000000", "1500000"}}) {
            Input in = new Input(1, "NV", "S", "Sales", "T", false, "REGION_I", List.of(seg("9000000", 176, 176)), List.of(), List.of(), true,
                    d((String) c[0]), 176 * 60, 176 * 60, ins("9000000"), PROG, 0, 0, List.of(), List.of(), List.of(), List.of());
            Result r = PayrollEngine.calculate(in, P);
            assertEquals(d((String) c[1]), r.commission());
            assertEquals(d((String) c[2]), r.kpiBonus());
        }
        // Sales nửa tháng, doanh thu 60 triệu => chỉ tiêu 50 triệu, hoa hồng 1,2 triệu, KPI 1,5 triệu
        Input half = new Input(1, "NV", "S", "Sales", "T", false, "REGION_I", List.of(seg("9000000", 88, 176)), List.of(), List.of(), true,
                d("60000000"), 88 * 60, 176 * 60, ins("9000000"), PROG, 0, 0, List.of(), List.of(), List.of(), List.of());
        Result r = PayrollEngine.calculate(half, P);
        assertEquals(d("1200000"), r.commission());
        assertEquals(d("1500000"), r.kpiBonus());
    }

    @Test
    void b13b14ProgressiveBrackets() {
        String[][] cases = {{"0", "0"}, {"10000000", "500000"}, {"30000000", "2500000"}, {"60000000", "8500000"}, {"100000000", "20500000"}};
        for (String[] c : cases) {
            assertEquals(d(c[1]), PayrollEngine.progressive(d(c[0]), P.brackets()));
        }
        Input in = new Input(1, "NV", "T", "D", "T", false, "REGION_I", List.of(seg("40000000", 176, 176)), List.of(), List.of(), false,
                BigDecimal.ZERO, 0, 0, ins("1000000"), PROG, 1, 0, List.of(), List.of(), List.of(), List.of());
        assertEquals(d("21700000"), PayrollEngine.calculate(in, P).familyRelief());
    }

    @Test
    void missingProfilesAreFlaggedNotZeroed() {
        Input in = new Input(1, "NV", "T", "D", "T", false, "REGION_I", List.of(seg("10000000", 176, 176)), List.of(), List.of(), false,
                BigDecimal.ZERO, 0, 0, null, null, 0, 0, List.of(), List.of(), List.of(), List.of());
        assertEquals("NEEDS_REVIEW", PayrollEngine.calculate(in, P).status());
    }

    @Test
    void roundingHalfUpAppliesToNegatives() {
        assertEquals(d("1"), PayrollEngine.vnd(d("0.5")));
        assertEquals(d("-1"), PayrollEngine.vnd(d("-0.5")));
    }
}
