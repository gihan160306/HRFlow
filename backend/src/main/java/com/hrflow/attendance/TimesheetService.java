package com.hrflow.attendance;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.hrflow.attendance.WorkCalendar.DayPlan;
import com.hrflow.common.util.Vn;

/**
 * Bảng công theo ngày của một nhân viên: kết hợp lịch cá nhân, ngày lễ, chấm công, nghỉ phép đã duyệt,
 * thời gian hợp đồng hiệu lực. Là nguồn duy nhất cho màn hình công và cho bước chốt công (snapshot).
 * Một phút chỉ được trả một lần: phút trả = min(giờ theo lịch, phút làm + phút nghỉ hưởng lương).
 */
@Service
public class TimesheetService {

    public record Day(LocalDate date, String dayType, String holidayName, Long scheduleId, int scheduledMinutes,
                      Long attendanceId, Instant checkInAt, Instant checkOutAt, int workedMinutes, int lateMinutes,
                      int earlyMinutes, String attendanceStatus, String method, int paidLeaveMinutes,
                      int unpaidLeaveMinutes, boolean pendingLeave, int paidMinutes, boolean inEmployment,
                      String issue, String issueLevel) {
    }

    private final JdbcTemplate jdbc;
    private final WorkCalendar calendar;

    public TimesheetService(JdbcTemplate jdbc, WorkCalendar calendar) {
        this.jdbc = jdbc;
        this.calendar = calendar;
    }

    private record Rec(long id, Instant in, Instant out, String method, int worked, int late, int early, String status,
                       String reason) {
    }

    private record Cover(LocalDate from, LocalDate to) {
        boolean has(LocalDate d) {
            return !from.isAfter(d) && (to == null || !to.isBefore(d));
        }
    }

    public List<Day> days(long employeeId, LocalDate from, LocalDate to) {
        Map<String, Object> emp = jdbc.queryForMap("SELECT hire_date, termination_date FROM employee WHERE id = ?", employeeId);
        LocalDate hire = ((java.sql.Date) emp.get("hire_date")).toLocalDate();
        LocalDate term = emp.get("termination_date") == null ? null : ((java.sql.Date) emp.get("termination_date")).toLocalDate();
        List<Cover> contracts = jdbc.query("""
                SELECT start_date, COALESCE(termination_date, end_date) AS stop FROM contract
                WHERE employee_id = ? AND status IN ('ACTIVE', 'EXPIRED', 'TERMINATED')
                """, (rs, i) -> new Cover(rs.getDate(1).toLocalDate(), rs.getDate(2) == null ? null : rs.getDate(2).toLocalDate()),
                employeeId);
        Map<LocalDate, DayPlan> plans = calendar.plans(employeeId, from, to);

        Map<LocalDate, Rec> recs = new HashMap<>();
        jdbc.query("""
                SELECT id, work_date, check_in_at, check_out_at, method, worked_minutes, late_minutes, early_minutes, status, review_reason
                FROM attendance_record WHERE employee_id = ? AND work_date BETWEEN ? AND ?
                """, rs -> {
            Timestamp in = rs.getTimestamp("check_in_at");
            Timestamp out = rs.getTimestamp("check_out_at");
            recs.put(rs.getDate("work_date").toLocalDate(), new Rec(rs.getLong("id"), in == null ? null : in.toInstant(),
                    out == null ? null : out.toInstant(), rs.getString("method"), rs.getInt("worked_minutes"),
                    rs.getInt("late_minutes"), rs.getInt("early_minutes"), rs.getString("status"), rs.getString("review_reason")));
        }, employeeId, from, to);

        Map<LocalDate, int[]> leave = new HashMap<>(); // [paid, unpaid]
        Map<LocalDate, Boolean> pending = new HashMap<>();
        jdbc.query("""
                SELECT d.leave_date, d.minutes, t.paid, r.status
                FROM leave_request_day d JOIN leave_request r ON r.id = d.leave_request_id
                JOIN leave_type t ON t.id = r.leave_type_id
                WHERE r.employee_id = ? AND r.kind = 'NEW' AND r.status IN ('APPROVED', 'PENDING')
                  AND d.leave_date BETWEEN ? AND ?
                """, rs -> {
            LocalDate date = rs.getDate("leave_date").toLocalDate();
            if ("PENDING".equals(rs.getString("status"))) {
                pending.put(date, true);
            } else {
                int[] v = leave.computeIfAbsent(date, k -> new int[2]);
                v[rs.getBoolean("paid") ? 0 : 1] += rs.getInt("minutes");
            }
        }, employeeId, from, to);

        LocalDate today = Vn.today();
        List<Day> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DayPlan plan = plans.get(d);
            boolean hireWindow = !d.isBefore(hire) && (term == null || !d.isAfter(term));
            final LocalDate day = d;
            boolean covered = contracts.stream().anyMatch(c -> c.has(day));
            boolean inEmployment = hireWindow && covered;
            Rec rec = recs.get(d);
            int[] lv = leave.getOrDefault(d, new int[2]);
            boolean pend = pending.getOrDefault(d, false);
            int scheduled = plan.scheduledMinutes();

            int worked = rec == null ? 0 : Math.min(rec.worked(), scheduled);
            String status = rec == null ? null : rec.status();
            String reason = rec == null ? null : rec.reason();
            // Bản ghi đang mở của ngày đã qua = thiếu check-out -> cần rà soát.
            if (rec != null && "VALID".equals(status) && d.isBefore(today) && (rec.in() == null || rec.out() == null)) {
                status = "NEEDS_REVIEW";
                reason = rec.in() == null ? "Thiếu check-in" : "Thiếu check-out";
            }
            int paid = !inEmployment || scheduled == 0 ? 0 : Math.min(scheduled, worked + lv[0]);
            int unpaidLeave = !inEmployment ? 0 : Math.min(lv[1], scheduled);

            String issue = null;
            String level = null;
            if (hireWindow && !covered && scheduled > 0 && !d.isAfter(today)) {
                issue = "Không có hợp đồng hiệu lực trong ngày làm việc";
                level = "BLOCKING";
            } else if (inEmployment && scheduled > 0 && !d.isAfter(today)) {
                if (pend) {
                    issue = "Đơn nghỉ phép đang chờ duyệt";
                    level = "BLOCKING";
                } else if (rec == null) {
                    boolean leaveCovers = lv[0] + lv[1] >= scheduled;
                    if (!leaveCovers && d.isBefore(today)) {
                        issue = "Thiếu dữ liệu chấm công (chưa có công, nghỉ hay vắng được xác nhận)";
                        level = "BLOCKING";
                    }
                } else if ("NEEDS_REVIEW".equals(status)) {
                    issue = reason == null ? "Cần rà soát chấm công" : reason;
                    level = "BLOCKING";
                }
            }
            out.add(new Day(d, plan.dayType(), plan.holidayName(), plan.scheduleId(), scheduled,
                    rec == null ? null : rec.id(), rec == null ? null : rec.in(), rec == null ? null : rec.out(),
                    worked, rec == null ? 0 : rec.late(), rec == null ? 0 : rec.early(), status,
                    rec == null ? null : rec.method(), inEmployment ? lv[0] : 0, unpaidLeave, pend, paid, inEmployment,
                    issue, level));
        }
        return out;
    }

    public int blockingCount(List<Day> days) {
        return (int) days.stream().filter(x -> "BLOCKING".equals(x.issueLevel())).count();
    }
}
