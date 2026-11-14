package com.hrflow.payroll;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.hrflow.attendance.TimesheetService;
import com.hrflow.attendance.WorkCalendar;
import com.hrflow.attendance.WorkCalendar.DayPlan;
import com.hrflow.common.util.Vn;
import com.hrflow.payroll.PayrollEngine.AdjIn;
import com.hrflow.payroll.PayrollEngine.AllowanceIn;
import com.hrflow.payroll.PayrollEngine.AllowancePart;
import com.hrflow.payroll.PayrollEngine.InsuranceIn;
import com.hrflow.payroll.PayrollEngine.Input;
import com.hrflow.payroll.PayrollEngine.ItemIn;
import com.hrflow.payroll.PayrollEngine.OtSeg;
import com.hrflow.payroll.PayrollEngine.SalarySeg;
import com.hrflow.payroll.PayrollEngine.TaxIn;

/**
 * Chốt công: gom MỌI dữ liệu nguồn của kỳ từ DB (công, nghỉ, OT, phụ cấp, lương, bảo hiểm, thuế, thu nhập,
 * doanh thu Sales, điều chỉnh) thành một {@link Input} bất biến cho từng nhân viên. Bước tính lương chỉ đọc snapshot
 * này, không nhận số liệu do client gửi lên.
 */
@Service
public class SnapshotBuilder {

    private final JdbcTemplate jdbc;
    private final WorkCalendar calendar;
    private final TimesheetService timesheets;

    public SnapshotBuilder(JdbcTemplate jdbc, WorkCalendar calendar, TimesheetService timesheets) {
        this.jdbc = jdbc;
        this.calendar = calendar;
        this.timesheets = timesheets;
    }

    /** Nhân viên có thời gian làm việc chồng với kỳ (kể cả người đã nghỉ việc trong kỳ). */
    public List<Long> employeesOf(LocalDate start, LocalDate end) {
        return jdbc.queryForList("""
                SELECT id FROM employee WHERE hire_date <= ? AND (termination_date IS NULL OR termination_date >= ?) ORDER BY code
                """, Long.class, end, start);
    }

    private record Seg(LocalDate from, LocalDate to, BigDecimal salary, Long scheduleId, long std) {
    }

    public Input build(long employeeId, long periodId, int year, int month, LocalDate start, LocalDate end, LocalDate payDate) {
        Map<String, Object> e = jdbc.queryForMap("""
                SELECT e.code, e.full_name, e.wage_region, e.termination_date, e.hire_date, d.name AS dept, j.name AS title, j.code AS title_code,
                       j.sales_owner_eligible
                FROM employee e JOIN department d ON d.id = e.department_id JOIN job_title j ON j.id = e.job_title_id WHERE e.id = ?
                """, employeeId);
        List<String> issues = new ArrayList<>();
        List<TimesheetService.Day> days = timesheets.days(employeeId, start, end);
        for (TimesheetService.Day d : days) {
            if ("BLOCKING".equals(d.issueLevel())) {
                issues.add(d.date() + ": " + d.issue());
            }
        }

        // --- đoạn hiệu lực: đổi lương hoặc đổi lịch giữa kỳ ---
        TreeSet<LocalDate> breaks = new TreeSet<>();
        breaks.add(start);
        List<Map<String, Object>> salaries = jdbc.queryForList(
                "SELECT base_salary, effective_from, effective_to FROM salary_record WHERE employee_id = ? ORDER BY effective_from", employeeId);
        for (Map<String, Object> s : salaries) {
            LocalDate f = ((java.sql.Date) s.get("effective_from")).toLocalDate();
            if (f.isAfter(start) && !f.isAfter(end)) {
                breaks.add(f);
            }
        }
        Map<LocalDate, DayPlan> plans = calendar.plans(employeeId, start, end);
        Long prevSchedule = plans.get(start).scheduleId();
        for (LocalDate d = start.plusDays(1); !d.isAfter(end); d = d.plusDays(1)) {
            Long sid = plans.get(d).scheduleId();
            if (!java.util.Objects.equals(sid, prevSchedule)) {
                breaks.add(d);
                prevSchedule = sid;
            }
        }
        List<LocalDate> bl = new ArrayList<>(breaks);
        List<Seg> segs = new ArrayList<>();
        Map<Long, Long> stdCache = new HashMap<>();
        for (int i = 0; i < bl.size(); i++) {
            LocalDate from = bl.get(i);
            LocalDate to = i + 1 < bl.size() ? bl.get(i + 1).minusDays(1) : end;
            BigDecimal salary = salaryAt(salaries, from);
            Long sid = plans.get(from).scheduleId();
            long std = sid == null ? 0 : stdCache.computeIfAbsent(sid, k -> (long) calendar.stdMinutes(k, start, end));
            segs.add(new Seg(from, to, salary == null ? BigDecimal.ZERO : salary, sid, std));
            if (salary == null && days.stream().anyMatch(x -> !x.date().isBefore(from) && !x.date().isAfter(to) && x.paidMinutes() > 0)) {
                issues.add("Thiếu mức lương cơ bản có hiệu lực từ " + from);
            }
        }
        List<SalarySeg> salarySegs = new ArrayList<>();
        for (Seg s : segs) {
            long paid = days.stream().filter(x -> !x.date().isBefore(s.from()) && !x.date().isAfter(s.to())).mapToLong(TimesheetService.Day::paidMinutes).sum();
            salarySegs.add(new SalarySeg(s.from(), s.to(), s.salary(), paid, s.std(), "salary_record@" + s.from()));
        }

        // --- phụ cấp: giờ đủ điều kiện theo lịch, trừ nghỉ không lương/vắng không phép ---
        List<AllowanceIn> allowances = new ArrayList<>();
        List<Map<String, Object>> alRows = jdbc.queryForList("""
                SELECT ea.amount, ea.effective_from, ea.effective_to, ea.status, t.code, t.name, t.tax_class, t.included_in_ot_base
                FROM employee_allowance ea JOIN allowance_type t ON t.id = ea.allowance_type_id
                WHERE ea.employee_id = ? AND ea.effective_from <= ? AND COALESCE(ea.effective_to, DATE '9999-12-31') >= ?
                ORDER BY t.code, ea.effective_from
                """, employeeId, end, start);
        Map<String, List<AllowancePart>> partsByCode = new LinkedHashMap<>();
        Map<String, Map<String, Object>> meta = new HashMap<>();
        for (Map<String, Object> al : alRows) {
            LocalDate af = ((java.sql.Date) al.get("effective_from")).toLocalDate();
            LocalDate at = al.get("effective_to") == null ? LocalDate.of(9999, 12, 31) : ((java.sql.Date) al.get("effective_to")).toLocalDate();
            String code = (String) al.get("code") + "#" + af;
            meta.put(code, al);
            for (Seg s : segs) {
                long elig = 0;
                for (TimesheetService.Day d : days) {
                    if (d.date().isBefore(s.from()) || d.date().isAfter(s.to()) || d.date().isBefore(af) || d.date().isAfter(at) || !d.inEmployment()) {
                        continue;
                    }
                    long absent = "ABSENT_CONFIRMED".equals(d.attendanceStatus())
                            ? Math.max(0, d.scheduledMinutes() - d.paidLeaveMinutes() - d.unpaidLeaveMinutes()) : 0;
                    elig += Math.max(0, d.scheduledMinutes() - d.unpaidLeaveMinutes() - absent);
                }
                partsByCode.computeIfAbsent(code, k -> new ArrayList<>())
                        .add(new AllowancePart((BigDecimal) al.get("amount"), elig, s.std()));
            }
        }
        Map<String, AllowanceIn> merged = new LinkedHashMap<>();
        for (Map.Entry<String, List<AllowancePart>> en : partsByCode.entrySet()) {
            Map<String, Object> al = meta.get(en.getKey());
            String code = (String) al.get("code");
            AllowanceIn existing = merged.get(code);
            List<AllowancePart> all = new ArrayList<>(existing == null ? List.of() : existing.parts());
            all.addAll(en.getValue());
            String status = existing != null && "NEEDS_REVIEW".equals(existing.status()) ? "NEEDS_REVIEW" : (String) al.get("status");
            merged.put(code, new AllowanceIn(code, (String) al.get("name"), (String) al.get("tax_class"), status, all));
        }
        allowances.addAll(merged.values());

        // --- OT ---
        List<OtSeg> ot = otSegments(employeeId, start, end, segs, alRows, issues);

        // --- Sales ---
        boolean salesEligible = Boolean.TRUE.equals(e.get("sales_owner_eligible"));
        List<Map<String, Object>> assigns = jdbc.queryForList("""
                SELECT a.effective_from, a.effective_to, j.sales_owner_eligible FROM employee_assignment a JOIN job_title j ON j.id = a.job_title_id
                WHERE a.employee_id = ? AND a.effective_from <= ? AND COALESCE(a.effective_to, DATE '9999-12-31') >= ?
                """, employeeId, end, start);
        long salesMinutes = 0;
        for (TimesheetService.Day d : days) {
            if (!d.inEmployment()) {
                continue;
            }
            boolean owner = false;
            boolean found = false;
            for (Map<String, Object> a : assigns) {
                LocalDate f = ((java.sql.Date) a.get("effective_from")).toLocalDate();
                LocalDate t = a.get("effective_to") == null ? LocalDate.of(9999, 12, 31) : ((java.sql.Date) a.get("effective_to")).toLocalDate();
                if (!d.date().isBefore(f) && !d.date().isAfter(t)) {
                    owner = Boolean.TRUE.equals(a.get("sales_owner_eligible"));
                    found = true;
                }
            }
            if (!found) {
                owner = Boolean.TRUE.equals(e.get("sales_owner_eligible"));
            }
            if (owner) {
                salesMinutes += d.scheduledMinutes();
                salesEligible = true;
            }
        }
        BigDecimal revenue = jdbc.queryForObject("""
                SELECT COALESCE(SUM(eligible_revenue), 0) FROM sales_revenue
                WHERE owner_employee_id = ? AND period_year = ? AND period_month = ? AND status = 'CONFIRMED'
                """, BigDecimal.class, employeeId, year, month);
        long fullStd = segs.stream().mapToLong(Seg::std).max().orElse(0);

        // --- bảo hiểm / thuế / người phụ thuộc ---
        LocalDate term = e.get("termination_date") == null ? null : ((java.sql.Date) e.get("termination_date")).toLocalDate();
        LocalDate ref = term != null && term.isBefore(end) ? term : end;
        InsuranceIn insurance = jdbc.query("""
                SELECT bhxh, bhyt, bhtn, insurance_salary, status FROM insurance_profile
                WHERE employee_id = ? AND effective_from <= ? AND COALESCE(effective_to, DATE '9999-12-31') >= ?
                ORDER BY effective_from DESC, id DESC LIMIT 1
                """, rs -> rs.next() ? new InsuranceIn(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBigDecimal(4), rs.getString(5)) : null,
                employeeId, ref, ref);
        TaxIn tax = jdbc.query("""
                SELECT residency, method, status, commitment_08, commitment_doc_ref FROM tax_profile
                WHERE employee_id = ? AND effective_from <= ? AND COALESCE(effective_to, DATE '9999-12-31') >= ?
                ORDER BY effective_from DESC, id DESC LIMIT 1
                """, rs -> rs.next() ? new TaxIn(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getString(5)) : null,
                employeeId, payDate.isBefore(ref) ? ref : payDate, payDate.isBefore(ref) ? ref : payDate);
        if (tax == null) {
            tax = jdbc.query("""
                    SELECT residency, method, status, commitment_08, commitment_doc_ref FROM tax_profile
                    WHERE employee_id = ? AND effective_from <= ? AND COALESCE(effective_to, DATE '9999-12-31') >= ?
                    ORDER BY effective_from DESC, id DESC LIMIT 1
                    """, rs -> rs.next() ? new TaxIn(rs.getString(1), rs.getString(2), rs.getString(3), rs.getBoolean(4), rs.getString(5)) : null,
                    employeeId, ref, ref);
        }
        int dependents = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dependent WHERE employee_id = ? AND verified AND valid_from <= ? AND COALESCE(valid_to, DATE '9999-12-31') >= ?
                """, Integer.class, employeeId, end, start);
        int unverified = jdbc.queryForObject("""
                SELECT COUNT(*) FROM dependent WHERE employee_id = ? AND NOT verified AND valid_from <= ? AND COALESCE(valid_to, DATE '9999-12-31') >= ?
                """, Integer.class, employeeId, end, start);

        // --- nghỉ không lương dài ngày: cần hồ sơ báo giảm bảo hiểm, không tự chia tỷ lệ ---
        double unpaidDays = days.stream().filter(x -> x.scheduledMinutes() > 0).mapToDouble(x -> (double) x.unpaidLeaveMinutes() / x.scheduledMinutes()).sum();
        if (unpaidDays >= 14) {
            issues.add("Nghỉ không lương ≥ 14 ngày làm việc: cần hồ sơ báo giảm/thỏa thuận tiếp tục đóng BHXH, HR xác nhận từng quỹ");
        }

        // --- thu nhập khác, điều chỉnh ---
        List<ItemIn> items = jdbc.query("""
                SELECT item_type, name, amount, tax_class, source_ref FROM income_item
                WHERE employee_id = ? AND period_year = ? AND period_month = ? AND status = 'CONFIRMED' ORDER BY id
                """, (rs, i) -> new ItemIn(rs.getString(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4), rs.getString(5)),
                employeeId, year, month);
        List<AdjIn> adjs = jdbc.query("""
                SELECT id, adj_type, amount, tax_class, source_ref FROM payroll_adjustment
                WHERE employee_id = ? AND status = 'APPROVED' AND (applied_period_id IS NULL OR applied_period_id = ?) ORDER BY id
                """, (rs, i) -> new AdjIn(rs.getLong(1), rs.getString(2), rs.getBigDecimal(3), rs.getString(4), rs.getString(5)),
                employeeId, periodId);

        List<Map<String, Object>> dayRows = new ArrayList<>();
        for (TimesheetService.Day d : days) {
            if (d.scheduledMinutes() > 0 || d.attendanceId() != null) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("date", d.date().toString());
                m.put("dayType", d.dayType());
                m.put("scheduled", d.scheduledMinutes());
                m.put("worked", d.workedMinutes());
                m.put("paidLeave", d.paidLeaveMinutes());
                m.put("unpaidLeave", d.unpaidLeaveMinutes());
                m.put("paid", d.paidMinutes());
                m.put("late", d.lateMinutes());
                m.put("early", d.earlyMinutes());
                m.put("status", d.attendanceStatus());
                dayRows.add(m);
            }
        }
        return new Input(employeeId, (String) e.get("code"), (String) e.get("full_name"), (String) e.get("dept"), (String) e.get("title"),
                "DIRECTOR".equals(e.get("title_code")), (String) e.get("wage_region"), salarySegs, allowances, ot, salesEligible,
                revenue, salesMinutes, fullStd, insurance, tax, dependents, unverified, items, adjs, issues, dayRows);
    }

    private static BigDecimal salaryAt(List<Map<String, Object>> salaries, LocalDate d) {
        BigDecimal v = null;
        for (Map<String, Object> s : salaries) {
            LocalDate f = ((java.sql.Date) s.get("effective_from")).toLocalDate();
            LocalDate t = s.get("effective_to") == null ? LocalDate.of(9999, 12, 31) : ((java.sql.Date) s.get("effective_to")).toLocalDate();
            if (!f.isAfter(d) && !t.isBefore(d)) {
                v = (BigDecimal) s.get("base_salary");
            }
        }
        return v;
    }

    // ---------------- OT: tách đoạn theo 00:00 / 06:00 / 22:00 và loại ngày ----------------

    private record PaidInterval(String ref, Instant start, Instant end) {
    }

    private List<OtSeg> otSegments(long employeeId, LocalDate start, LocalDate end, List<Seg> segs, List<Map<String, Object>> allowanceRows,
                                   List<String> issues) {
        List<PaidInterval> paid = new ArrayList<>();
        jdbc.query("""
                SELECT r.id, r.actual_start, r.actual_minutes, r.approved_minutes, r.extra_minutes_confirmed FROM ot_registration r
                WHERE r.employee_id = ? AND r.status = 'APPROVED' AND r.actual_status = 'CONFIRMED' AND r.actual_start IS NOT NULL
                  AND r.actual_start >= ? AND r.actual_start < ?
                """, rs -> {
            int actual = rs.getInt("actual_minutes");
            int payable = Math.min(actual, rs.getInt("approved_minutes")) + rs.getInt("extra_minutes_confirmed");
            Instant s = rs.getTimestamp("actual_start").toInstant();
            if (payable > 0) {
                paid.add(new PaidInterval("OTREG-" + rs.getLong("id"), s, s.plusSeconds(payable * 60L)));
            }
        }, employeeId, java.sql.Timestamp.from(start.atStartOfDay(Vn.ZONE).toInstant()), java.sql.Timestamp.from(end.plusDays(1).atStartOfDay(Vn.ZONE).toInstant()));
        jdbc.query("""
                SELECT id, start_at, end_at FROM ot_unscheduled WHERE employee_id = ? AND status = 'CONFIRMED' AND work_date BETWEEN ? AND ?
                """, rs -> {
            paid.add(new PaidInterval("OTUNS-" + rs.getLong("id"), rs.getTimestamp("start_at").toInstant(), rs.getTimestamp("end_at").toInstant()));
        }, employeeId, start, end);
        paid.sort((a, b) -> a.start().compareTo(b.start()));

        Map<LocalDate, DayPlan> plans = calendar.plans(employeeId, start.minusDays(1), end.plusDays(1));
        Map<LocalDate, Boolean> dayOtSeen = new HashMap<>();
        List<OtSeg> out = new ArrayList<>();
        long totalMinutes = 0;
        for (PaidInterval iv : paid) {
            LocalDateTime cur = iv.start().atZone(Vn.ZONE).toLocalDateTime();
            LocalDateTime stop = iv.end().atZone(Vn.ZONE).toLocalDateTime();
            while (cur.isBefore(stop)) {
                LocalDateTime next = nextBoundary(cur);
                if (next.isAfter(stop)) {
                    next = stop;
                }
                LocalDate date = cur.toLocalDate();
                DayPlan plan = plans.get(date);
                boolean night = cur.getHour() >= 22 || cur.getHour() < 6;
                String dayType = plan == null ? "WEEKDAY" : WorkCalendar.HOLIDAY.equals(plan.dayType()) ? "HOLIDAY"
                        : plan.scheduledMinutes() == 0 ? "WEEKLY_OFF" : "WEEKDAY";
                long minutes = Duration.between(cur, next).toMinutes();
                if ("WEEKDAY".equals(dayType) && plan != null) {
                    // phút trùng giờ làm theo lịch đã được trả ở lương theo công: một phút chỉ trả một lần
                    for (WorkCalendar.Interval w : plan.intervals()) {
                        LocalDateTime ws = date.atTime(w.start());
                        LocalDateTime we = date.atTime(w.end());
                        LocalDateTime a = ws.isAfter(cur) ? ws : cur;
                        LocalDateTime b = we.isBefore(next) ? we : next;
                        if (b.isAfter(a)) {
                            minutes -= Duration.between(a, b).toMinutes();
                        }
                    }
                }
                if (minutes > 0) {
                    BigDecimal kDay;
                    if (night) {
                        kDay = switch (dayType) {
                            case "HOLIDAY" -> new BigDecimal("3");
                            case "WEEKLY_OFF" -> new BigDecimal("2");
                            default -> Boolean.TRUE.equals(dayOtSeen.get(date)) ? new BigDecimal("1.5") : BigDecimal.ONE;
                        };
                    } else {
                        kDay = BigDecimal.ONE;
                        dayOtSeen.put(date, true);
                    }
                    Seg seg = segs.stream().filter(s -> !date.isBefore(s.from()) && !date.isAfter(s.to())).findFirst().orElse(segs.get(segs.size() - 1));
                    BigDecimal base = seg.salary();
                    for (Map<String, Object> al : allowanceRows) {
                        LocalDate f = ((java.sql.Date) al.get("effective_from")).toLocalDate();
                        LocalDate t = al.get("effective_to") == null ? LocalDate.of(9999, 12, 31) : ((java.sql.Date) al.get("effective_to")).toLocalDate();
                        if (Boolean.TRUE.equals(al.get("included_in_ot_base")) && !date.isBefore(f) && !date.isAfter(t)) {
                            base = base.add((BigDecimal) al.get("amount"));
                        }
                    }
                    out.add(new OtSeg(iv.ref(), dayType, night, kDay, minutes, base, seg.std()));
                    totalMinutes += minutes;
                }
                cur = next;
            }
        }
        if (totalMinutes > 40 * 60) {
            issues.add("Lưu ý: tổng OT thực tế trong tháng vượt 40 giờ; phải xử lý trách nhiệm, không xóa tiền công");
        }
        return out;
    }

    private static LocalDateTime nextBoundary(LocalDateTime t) {
        LocalDate d = t.toLocalDate();
        LocalTime time = t.toLocalTime();
        if (time.isBefore(LocalTime.of(6, 0))) {
            return d.atTime(6, 0);
        }
        if (time.isBefore(LocalTime.of(22, 0))) {
            return d.atTime(22, 0);
        }
        return d.plusDays(1).atStartOfDay();
    }
}
