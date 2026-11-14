package com.hrflow.leave;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.hrflow.attendance.WorkCalendar.DayPlan;
import com.hrflow.attendance.WorkCalendar.Interval;
import com.hrflow.common.exception.BusinessException;

/**
 * Thời lượng nghỉ theo lịch làm việc cá nhân (payroll_rules Mục 3.1):
 * nghỉ nửa ngày tính theo thời lượng ca thực tế (sáng 3,5 giờ / chiều 4,5 giờ không bị coi là 4 giờ),
 * số ngày nghỉ = phút nghỉ / phút theo lịch của đúng ngày đó.
 */
public final class LeaveCalc {

    public record DayLeave(LocalDate date, int minutes, BigDecimal dayFraction) {
    }

    private LeaveCalc() {
    }

    public static List<DayLeave> compute(Map<LocalDate, DayPlan> plans, String unit, LocalDate start, LocalDate end,
                                         LocalTime startTime, LocalTime endTime) {
        List<DayLeave> out = new ArrayList<>();
        if (!"FULL_DAY".equals(unit) && !start.equals(end)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Nghỉ nửa ngày/theo giờ chỉ áp dụng cho một ngày");
        }
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DayPlan plan = plans.get(d);
            if (plan == null || plan.scheduledMinutes() == 0) {
                continue; // ngày nghỉ tuần/lễ không phải ngày làm việc nên không tính vào đơn nghỉ
            }
            int minutes = switch (unit) {
                case "FULL_DAY" -> plan.scheduledMinutes();
                case "HALF_DAY_AM" -> half(plan, true);
                case "HALF_DAY_PM" -> half(plan, false);
                case "HOURS" -> hours(plan, startTime, endTime);
                default -> throw BusinessException.unprocessable("VALIDATION_ERROR", "Đơn vị nghỉ không hợp lệ");
            };
            if (minutes > 0) {
                BigDecimal fraction = BigDecimal.valueOf(minutes).divide(BigDecimal.valueOf(plan.scheduledMinutes()), 4, RoundingMode.HALF_UP);
                out.add(new DayLeave(d, minutes, fraction));
            }
        }
        if (out.isEmpty()) {
            throw BusinessException.unprocessable("NO_WORKING_TIME", "Khoảng nghỉ không có giờ làm việc theo lịch cá nhân");
        }
        return out;
    }

    /** Ca có hai khoảng (sáng/chiều): lấy đúng khoảng đó; ca một khoảng: chia đôi thời lượng. */
    private static int half(DayPlan plan, boolean morning) {
        List<Interval> iv = plan.intervals();
        if (iv.size() >= 2) {
            return (morning ? iv.get(0) : iv.get(iv.size() - 1)).minutes();
        }
        int total = plan.scheduledMinutes();
        return morning ? total / 2 : total - total / 2;
    }

    private static int hours(DayPlan plan, LocalTime from, LocalTime to) {
        if (from == null || to == null || !to.isAfter(from)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Nghỉ theo giờ phải có giờ bắt đầu và giờ kết thúc hợp lệ");
        }
        int total = 0;
        for (Interval iv : plan.intervals()) {
            LocalTime s = iv.start().isAfter(from) ? iv.start() : from;
            LocalTime e = iv.end().isBefore(to) ? iv.end() : to;
            if (e.isAfter(s)) {
                total += (int) Duration.between(s, e).toMinutes();
            }
        }
        return total;
    }
}
