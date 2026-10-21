package com.hrflow.attendance;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.employee.Employee;
import com.hrflow.employee.EmployeeRepository;
import com.hrflow.security.AuthUser;

/**
 * Quy tắc công theo payroll_rules §5.2. Giờ chấm công luôn lấy từ đồng hồ server (không tin giờ client gửi).
 */
@Service
@Transactional(readOnly = true)
public class AttendanceService {

    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> HR_ADMIN = Set.of("HR", "ADMIN");
    private static final int MAX_QUERY_DAYS = 93;
    private static final BigDecimal CREDIT_FULL = new BigDecimal("1.0");
    private static final BigDecimal CREDIT_NONE = new BigDecimal("0.0");

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    private final LocalTime workStart;
    private final LocalTime workEnd;
    private final LocalTime lunchStart;
    private final LocalTime lunchEnd;
    private final int shortfallToleranceMinutes;
    private final int standardMinutes;

    // TODO: chuyển sang đọc system_parameter (work_start, work_end, lunch_break, daily_shortfall_tolerance_minutes).
    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             @Value("${hrflow.attendance.work-start:08:30}") String workStart,
                             @Value("${hrflow.attendance.work-end:17:30}") String workEnd,
                             @Value("${hrflow.attendance.lunch-start:12:00}") String lunchStart,
                             @Value("${hrflow.attendance.lunch-end:13:00}") String lunchEnd,
                             @Value("${hrflow.attendance.shortfall-tolerance-minutes:0}") int shortfallToleranceMinutes) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.workStart = LocalTime.parse(workStart);
        this.workEnd = LocalTime.parse(workEnd);
        this.lunchStart = LocalTime.parse(lunchStart);
        this.lunchEnd = LocalTime.parse(lunchEnd);
        this.shortfallToleranceMinutes = shortfallToleranceMinutes;
        // 08:30–17:30 trừ nghỉ trưa 12:00–13:00 = 480 phút
        this.standardMinutes = (int) (Duration.between(this.workStart, this.workEnd).toMinutes()
                - Duration.between(this.lunchStart, this.lunchEnd).toMinutes());
    }

    // ===================== Chấm công =====================

    @Transactional
    public AttendanceResponse checkIn(AuthUser user) {
        Long employeeId = requireEmployeeId(user);
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", employeeId));
        if (employee.isTerminated()) {
            throw BusinessException.conflict("EMPLOYEE_TERMINATED", "Nhân viên đã nghỉ việc");
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, VN);

        if (attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today).isPresent()) {
            throw BusinessException.conflict("ALREADY_CHECKED_IN",
                    "Hôm nay đã có bản ghi công. Nếu cần chỉnh sửa, hãy liên hệ HR.");
        }

        Attendance a = new Attendance();
        a.setEmployee(employee);
        a.setWorkDate(today);
        a.setCheckInAt(now);
        a.setSource(Attendance.Source.WEB);
        recalculate(a);
        return AttendanceResponse.from(attendanceRepository.save(a));
    }

    @Transactional
    public AttendanceResponse checkOut(AuthUser user) {
        Long employeeId = requireEmployeeId(user);
        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, VN);

        Attendance a = attendanceRepository.findByEmployeeIdAndWorkDate(employeeId, today)
                .filter(x -> x.getCheckInAt() != null)
                .orElseThrow(() -> BusinessException.conflict("NOT_CHECKED_IN", "Bạn chưa check-in hôm nay"));
        if (a.getCheckOutAt() != null) {
            throw BusinessException.conflict("ALREADY_CHECKED_OUT", "Bạn đã check-out hôm nay");
        }
        if (!now.isAfter(a.getCheckInAt())) {
            throw BusinessException.unprocessable("INVALID_TIME_RANGE", "Giờ ra phải sau giờ vào");
        }

        a.setCheckOutAt(now);
        recalculate(a);
        return AttendanceResponse.from(attendanceRepository.save(a));
    }

    // ===================== Tra cứu =====================

    /**
     * HR/Admin: mọi nhân viên. Manager: bản thân + team trực tiếp. Còn lại: chỉ bản thân.
     * Mặc định xem từ đầu tháng của `to` đến `to` (mặc định hôm nay); tối đa 93 ngày.
     */
    public List<AttendanceResponse> list(AuthUser user, Long employeeId, LocalDate from, LocalDate to) {
        LocalDate toDate = to != null ? to : LocalDate.now(VN);
        LocalDate fromDate = from != null ? from : toDate.withDayOfMonth(1);
        if (fromDate.isAfter(toDate)) {
            throw BusinessException.unprocessable("INVALID_DATE_RANGE", "from phải trước hoặc bằng to");
        }
        if (Duration.between(fromDate.atStartOfDay(), toDate.atStartOfDay()).toDays() > MAX_QUERY_DAYS) {
            throw BusinessException.unprocessable("DATE_RANGE_TOO_LARGE",
                    "Chỉ được tra cứu tối đa " + MAX_QUERY_DAYS + " ngày mỗi lần");
        }

        Specification<Attendance> spec = (root, q, cb) -> cb.between(root.get("workDate"), fromDate, toDate);
        Long me = user.employeeId();

        if (!isHrOrAdmin(user)) {
            if (me == null) throw new AccessDeniedException("Tài khoản chưa liên kết hồ sơ nhân viên");

            if ("MANAGER".equals(user.role())) {
                if (employeeId == null) {
                    spec = spec.and((root, q, cb) -> cb.or(
                            cb.equal(root.get("employee").get("id"), me),
                            cb.equal(root.get("employee").get("managerId"), me)));
                } else if (!employeeId.equals(me)) {
                    Employee target = employeeRepository.findById(employeeId)
                            .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", employeeId));
                    if (!me.equals(target.getManagerId())) {
                        throw new AccessDeniedException("Chỉ được xem bảng công của nhân viên trong team");
                    }
                }
            } else {
                if (employeeId != null && !employeeId.equals(me)) {
                    throw new AccessDeniedException("Không có quyền xem bảng công của nhân viên khác");
                }
                spec = spec.and((root, q, cb) -> cb.equal(root.get("employee").get("id"), me));
            }
        }
        if (employeeId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "workDate").and(Sort.by("employee.employeeCode"));
        return attendanceRepository.findAll(spec, sort).stream().map(AttendanceResponse::from).toList();
    }

    // ===================== HR điều chỉnh =====================

    @Transactional
    public AttendanceResponse adjust(AuthUser user, AttendanceAdjustRequest r) {
        if (r.checkInAt() == null && r.checkOutAt() == null) {
            throw BusinessException.unprocessable("NOTHING_TO_ADJUST", "Cần nhập ít nhất giờ vào hoặc giờ ra");
        }
        Attendance a = attendanceRepository.findById(r.attendanceId())
                .orElseThrow(() -> new ResourceNotFoundException("Bản ghi công", r.attendanceId()));
        if (a.getExceptionDecision() != null) {
            throw BusinessException.conflict("ATTENDANCE_ALREADY_DECIDED",
                    "Ngày công đã có quyết định ngoại lệ của HR, không điều chỉnh giờ được");
        }

        Instant in = r.checkInAt() != null ? r.checkInAt().toInstant() : a.getCheckInAt();
        Instant out = r.checkOutAt() != null ? r.checkOutAt().toInstant() : a.getCheckOutAt();

        if (in == null) {
            throw BusinessException.unprocessable("CHECK_OUT_WITHOUT_CHECK_IN", "Không thể có giờ ra khi chưa có giờ vào");
        }
        if (out != null && !out.isAfter(in)) {
            throw BusinessException.unprocessable("INVALID_TIME_RANGE", "Giờ ra phải sau giờ vào");
        }
        for (Instant t : new Instant[]{in, out}) {
            if (t != null && !LocalDate.ofInstant(t, VN).equals(a.getWorkDate())) {
                throw BusinessException.unprocessable("TIME_OUTSIDE_WORK_DATE",
                        "Giờ vào/ra phải thuộc ngày công " + a.getWorkDate());
            }
        }

        a.setCheckInAt(in);
        a.setCheckOutAt(out);
        a.setSource(Attendance.Source.HR_CORRECTION);
        a.setNote(r.reason().trim());
        recalculate(a);
        // TODO: ghi audit_log (ADJUST attendance) với old/new check_in_at, check_out_at và lý do.
        return AttendanceResponse.from(attendanceRepository.save(a));
    }

    // ===================== Quy tắc tính công =====================

    /**
     * payroll_rules §5.2.
     * - late  = max(0, check_in - work_start); early = max(0, work_end - check_out)
     * - worked = (check_out - max(check_in, work_start)) - phần trùng nghỉ trưa, tối đa 480 phút.
     *   Giờ ra sau work_end vẫn được tính để bù giờ cho người vào trễ.
     * - shortfall = 480 - worked; shortfall ≤ dung sai → 1 công, ngược lại EXCEPTION chờ HR quyết định.
     * - Thứ Bảy/Chủ nhật (làm bù): không cộng công (day_credit = 0).
     */
    private void recalculate(Attendance a) {
        LocalDate d = a.getWorkDate();
        Instant start = d.atTime(workStart).atZone(VN).toInstant();
        Instant end = d.atTime(workEnd).atZone(VN).toInstant();
        Instant lunchS = d.atTime(lunchStart).atZone(VN).toInstant();
        Instant lunchE = d.atTime(lunchEnd).atZone(VN).toInstant();
        Instant in = a.getCheckInAt();
        Instant out = a.getCheckOutAt();

        a.setLateMinutes(in == null ? 0 : (int) Math.max(0, Duration.between(start, in).toMinutes()));

        if (in == null || out == null) {
            a.setEarlyMinutes(0);
            a.setWorkedMinutes(0);
            a.setDayCredit(CREDIT_NONE);
            a.setStatus(AttendanceStatus.OPEN);
            return;
        }

        a.setEarlyMinutes((int) Math.max(0, Duration.between(out, end).toMinutes()));

        Instant effectiveStart = in.isAfter(start) ? in : start;
        long gross = Math.max(0, Duration.between(effectiveStart, out).toMinutes());
        long worked = Math.min(standardMinutes, Math.max(0, gross - overlapMinutes(effectiveStart, out, lunchS, lunchE)));
        a.setWorkedMinutes((int) worked);

        DayOfWeek dow = d.getDayOfWeek();
        boolean weekend = dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;

        if (weekend) {
            a.setDayCredit(CREDIT_NONE);
            a.setStatus(AttendanceStatus.PRESENT);
        } else if (standardMinutes - worked <= shortfallToleranceMinutes) {
            a.setDayCredit(CREDIT_FULL);
            a.setStatus(AttendanceStatus.PRESENT);
        } else {
            a.setDayCredit(CREDIT_NONE);
            a.setStatus(AttendanceStatus.EXCEPTION);
        }
    }

    private static long overlapMinutes(Instant s1, Instant e1, Instant s2, Instant e2) {
        Instant s = s1.isAfter(s2) ? s1 : s2;
        Instant e = e1.isBefore(e2) ? e1 : e2;
        return e.isAfter(s) ? Duration.between(s, e).toMinutes() : 0;
    }

    private Long requireEmployeeId(AuthUser user) {
        if (user.employeeId() == null) {
            throw new AccessDeniedException("Tài khoản chưa liên kết hồ sơ nhân viên");
        }
        return user.employeeId(); // danh tính lấy từ JWT, không nhận employeeId từ client
    }

    private boolean isHrOrAdmin(AuthUser user) {
        return HR_ADMIN.contains(user.role());
    }
}