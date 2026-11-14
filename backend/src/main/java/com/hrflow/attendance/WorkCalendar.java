package com.hrflow.attendance;

import java.sql.Date;
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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.hrflow.common.util.Vn;

/**
 * Lịch làm việc cá nhân có hiệu lực theo ngày + ngày lễ (payroll_rules Mục 2.2, 3.1).
 * Không có "22 hay 26 công" cố định: số phút chuẩn luôn lấy từ lịch của từng nhân viên.
 */
@Component
public class WorkCalendar {

    public static final String WORKDAY = "WORKDAY";
    public static final String WEEKLY_OFF = "WEEKLY_OFF";
    public static final String HOLIDAY = "HOLIDAY";

    public record Interval(LocalTime start, LocalTime end) {
        public int minutes() {
            return (int) Duration.between(start, end).toMinutes();
        }
    }

    /** Kế hoạch làm việc của một ngày. scheduleId = null khi chưa có lịch hiệu lực. */
    public record DayPlan(LocalDate date, Long scheduleId, List<Interval> intervals, int scheduledMinutes,
                          String dayType, String holidayName) {
    }

    private final JdbcTemplate jdbc;

    public WorkCalendar(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ---------------- dữ liệu nền ----------------

    private Map<Long, Map<Integer, List<Interval>>> scheduleDays(java.util.Collection<Long> ids) {
        Map<Long, Map<Integer, List<Interval>>> out = new HashMap<>();
        for (Long id : ids) {
            Map<Integer, List<Interval>> byDow = new HashMap<>();
            jdbc.query("""
                    SELECT day_of_week, morning_start, morning_end, afternoon_start, afternoon_end
                    FROM work_schedule_day WHERE schedule_id = ?
                    """, rs -> {
                List<Interval> list = new ArrayList<>();
                if (rs.getTime("morning_start") != null && rs.getTime("morning_end") != null) {
                    list.add(new Interval(rs.getTime("morning_start").toLocalTime(), rs.getTime("morning_end").toLocalTime()));
                }
                if (rs.getTime("afternoon_start") != null && rs.getTime("afternoon_end") != null) {
                    list.add(new Interval(rs.getTime("afternoon_start").toLocalTime(), rs.getTime("afternoon_end").toLocalTime()));
                }
                byDow.put(rs.getInt("day_of_week"), list);
            }, id);
            out.put(id, byDow);
        }
        return out;
    }

    private record EmpSchedule(long scheduleId, LocalDate from, LocalDate to) {
    }

    private List<EmpSchedule> employeeSchedules(long employeeId, LocalDate from, LocalDate to) {
        return jdbc.query("""
                SELECT schedule_id, effective_from, effective_to FROM employee_schedule
                WHERE employee_id = ? AND effective_from <= ? AND COALESCE(effective_to, DATE '9999-12-31') >= ?
                ORDER BY effective_from, id
                """, (rs, i) -> new EmpSchedule(rs.getLong(1), rs.getDate(2).toLocalDate(),
                rs.getDate(3) == null ? null : rs.getDate(3).toLocalDate()), employeeId, to, from);
    }

    private Map<LocalDate, String> holidays(LocalDate from, LocalDate to) {
        Map<LocalDate, String> out = new HashMap<>();
        jdbc.query("SELECT holiday_date, name FROM holiday WHERE paid AND holiday_date BETWEEN ? AND ?",
                rs -> {
                    out.put(rs.getDate(1).toLocalDate(), rs.getString(2));
                }, from, to);
        return out;
    }

    // ---------------- kế hoạch ----------------

    /** Kế hoạch từng ngày trong [from, to] theo lịch cá nhân hiệu lực của nhân viên. */
    public Map<LocalDate, DayPlan> plans(long employeeId, LocalDate from, LocalDate to) {
        List<EmpSchedule> schedules = employeeSchedules(employeeId, from, to);
        Map<Long, Map<Integer, List<Interval>>> days = scheduleDays(schedules.stream().map(EmpSchedule::scheduleId).distinct().toList());
        Map<LocalDate, String> hol = holidays(from, to);
        Map<LocalDate, DayPlan> out = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Long scheduleId = null;
            for (EmpSchedule s : schedules) {
                if (!s.from().isAfter(d) && (s.to() == null || !s.to().isBefore(d))) {
                    scheduleId = s.scheduleId();
                }
            }
            out.put(d, plan(d, scheduleId, scheduleId == null ? null : days.get(scheduleId), hol.get(d)));
        }
        return out;
    }

    public DayPlan plan(long employeeId, LocalDate date) {
        return plans(employeeId, date, date).get(date);
    }

    private static DayPlan plan(LocalDate d, Long scheduleId, Map<Integer, List<Interval>> byDow, String holidayName) {
        List<Interval> intervals = byDow == null ? List.of() : byDow.getOrDefault(d.getDayOfWeek().getValue(), List.of());
        int minutes = intervals.stream().mapToInt(Interval::minutes).sum();
        if (holidayName != null) {
            // Lễ hưởng lương: không nằm trong giờ chuẩn, không cộng trùng công; làm ngày lễ tính OT riêng.
            return new DayPlan(d, scheduleId, List.of(), 0, HOLIDAY, holidayName);
        }
        return new DayPlan(d, scheduleId, intervals, minutes, minutes > 0 ? WORKDAY : WEEKLY_OFF, null);
    }

    /** Tổng phút chuẩn cả tháng theo một lịch cụ thể (loại lễ hưởng lương và ngày nghỉ tuần). */
    public int stdMinutes(long scheduleId, LocalDate from, LocalDate to) {
        Map<Long, Map<Integer, List<Interval>>> days = scheduleDays(List.of(scheduleId));
        Map<LocalDate, String> hol = holidays(from, to);
        int total = 0;
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            total += plan(d, scheduleId, days.get(scheduleId), hol.get(d)).scheduledMinutes();
        }
        return total;
    }

    // ---------------- tính phút ----------------

    /** Số phút [in, out] giao với các khoảng làm việc theo lịch của ngày. */
    public static int overlapMinutes(DayPlan plan, Instant in, Instant out) {
        if (in == null || out == null || !out.isAfter(in)) {
            return 0;
        }
        LocalDateTime i = in.atZone(Vn.ZONE).toLocalDateTime();
        LocalDateTime o = out.atZone(Vn.ZONE).toLocalDateTime();
        int total = 0;
        for (Interval iv : plan.intervals()) {
            LocalDateTime s = plan.date().atTime(iv.start());
            LocalDateTime e = plan.date().atTime(iv.end());
            LocalDateTime from = s.isAfter(i) ? s : i;
            LocalDateTime to = e.isBefore(o) ? e : o;
            if (to.isAfter(from)) {
                total += (int) Duration.between(from, to).toMinutes();
            }
        }
        return total;
    }

    /** Phút theo lịch bị bỏ lỡ từ đầu ngày đến thời điểm check-in (đi trễ, ghi nhận từ phút đầu, không dung sai). */
    public static int lateMinutes(DayPlan plan, Instant in) {
        if (in == null || plan.intervals().isEmpty()) {
            return 0;
        }
        LocalDateTime start = plan.date().atStartOfDay();
        return overlapMinutesLocal(plan, start, in.atZone(Vn.ZONE).toLocalDateTime());
    }

    /** Phút theo lịch còn lại sau thời điểm check-out (về sớm). */
    public static int earlyMinutes(DayPlan plan, Instant out) {
        if (out == null || plan.intervals().isEmpty()) {
            return 0;
        }
        LocalDateTime end = plan.date().plusDays(1).atStartOfDay();
        return overlapMinutesLocal(plan, out.atZone(Vn.ZONE).toLocalDateTime(), end);
    }

    private static int overlapMinutesLocal(DayPlan plan, LocalDateTime from, LocalDateTime to) {
        int total = 0;
        for (Interval iv : plan.intervals()) {
            LocalDateTime s = plan.date().atTime(iv.start());
            LocalDateTime e = plan.date().atTime(iv.end());
            LocalDateTime a = s.isAfter(from) ? s : from;
            LocalDateTime b = e.isBefore(to) ? e : to;
            if (b.isAfter(a)) {
                total += (int) Duration.between(a, b).toMinutes();
            }
        }
        return total;
    }
}
