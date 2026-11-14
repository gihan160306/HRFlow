package com.hrflow.attendance;

import java.time.Instant;
import java.time.LocalDate;

import com.hrflow.attendance.WorkCalendar.DayPlan;

/**
 * Tính công một ngày theo lịch cá nhân và theo phút (payroll_rules Mục 3.1):
 * <ul>
 *   <li>không dung sai tự động (đã bỏ shortfall-tolerance 15 phút); trễ/sớm ghi nhận từ phút đầu tiên;</li>
 *   <li>thiếu check-in/check-out, check-out trước check-in, chấm công ngoài lịch -&gt; NEEDS_REVIEW,
 *       tuyệt đối không tự coi là 0 công;</li>
 *   <li>công làm thường chỉ tính phần giao với giờ làm theo lịch; phần ngoài lịch thuộc OT riêng.</li>
 * </ul>
 */
public final class AttendanceCalc {

    public record Result(int scheduledMinutes, int workedMinutes, int lateMinutes, int earlyMinutes, String status,
                         String reviewReason) {
    }

    private AttendanceCalc() {
    }

    public static Result compute(DayPlan plan, Instant in, Instant out, String method, LocalDate today) {
        int scheduled = plan.scheduledMinutes();
        if ("ABSENCE".equals(method)) {
            return new Result(scheduled, 0, 0, 0, "ABSENT_CONFIRMED", null);
        }
        if ("BUSINESS_TRIP".equals(method)) {
            // Công tác/làm ngoài được người có thẩm quyền xác nhận: hưởng đủ giờ theo lịch.
            return new Result(scheduled, scheduled, 0, 0, "CONFIRMED", null);
        }
        if (in == null && out == null) {
            return new Result(scheduled, 0, 0, 0, "NEEDS_REVIEW", "Thiếu dữ liệu check-in/check-out");
        }
        if (in == null) {
            return new Result(scheduled, 0, 0, 0, "NEEDS_REVIEW", "Thiếu check-in");
        }
        if (out == null) {
            boolean open = !plan.date().isBefore(today);
            return new Result(scheduled, 0, WorkCalendar.lateMinutes(plan, in), 0,
                    open ? "VALID" : "NEEDS_REVIEW", open ? null : "Thiếu check-out");
        }
        if (!out.isAfter(in)) {
            return new Result(scheduled, 0, 0, 0, "NEEDS_REVIEW", "Giờ check-out không sau giờ check-in");
        }
        if (scheduled == 0) {
            return new Result(0, 0, 0, 0, "NEEDS_REVIEW",
                    "Chấm công ngoài lịch làm việc; giờ làm thêm phải đi theo đăng ký/xác minh OT");
        }
        int worked = WorkCalendar.overlapMinutes(plan, in, out);
        return new Result(scheduled, worked, WorkCalendar.lateMinutes(plan, in), WorkCalendar.earlyMinutes(plan, out),
                "VALID", null);
    }
}
