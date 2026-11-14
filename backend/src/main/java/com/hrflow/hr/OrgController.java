package com.hrflow.hr;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.util.Sql;
import com.hrflow.security.Principals;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Danh mục: phòng ban, chức danh, lịch làm việc, ngày lễ, loại phụ cấp, loại nghỉ, biểu thuế. */
@RestController
public class OrgController {

    public record DepartmentRequest(@NotBlank String code, @NotBlank String name, String description, Boolean active,
                                    @NotBlank String decisionRef) {
    }

    public record HolidayRequest(@NotNull LocalDate holidayDate, @NotBlank String name, Boolean paid, String note) {
    }

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final PeriodGuard guard;

    public OrgController(JdbcTemplate jdbc, AuditService audit, PeriodGuard guard) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
    }

    // ---- Phòng ban ----

    private static final String DEPT_SELECT = """
            SELECT d.id, d.code, d.name, d.description, d.active,
                   (SELECT COUNT(*) FROM employee e WHERE e.department_id = d.id AND e.status <> 'TERMINATED') AS headcount
            FROM department d
            """;

    @GetMapping("/api/v1/departments")
    public List<Map<String, Object>> departments() {
        Principals.current();
        return Sql.list(jdbc, DEPT_SELECT + " ORDER BY d.id");
    }

    @PostMapping("/api/v1/departments")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createDepartment(@Valid @RequestBody DepartmentRequest r) {
        Principals.requireRole("HR");
        Long id = jdbc.queryForObject("""
                INSERT INTO department(code, name, description, active) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, r.code().trim(), r.name().trim(), r.description(), r.active() == null || r.active());
        audit.success("CREATE_DEPARTMENT", "department", id, r.decisionRef(), null, Map.of("code", r.code(), "name", r.name()));
        return Sql.one(jdbc, "Không tìm thấy phòng ban", DEPT_SELECT + " WHERE d.id = ?", id);
    }

    @PatchMapping("/api/v1/departments/{id}")
    @Transactional
    public Map<String, Object> updateDepartment(@PathVariable long id, @Valid @RequestBody DepartmentRequest r) {
        Principals.requireRole("HR");
        Map<String, Object> before = Sql.one(jdbc, "Không tìm thấy phòng ban", DEPT_SELECT + " WHERE d.id = ?", id);
        boolean active = r.active() == null || r.active();
        if (!active && ((Number) before.get("headcount")).longValue() > 0) {
            throw BusinessException.conflict("DEPARTMENT_NOT_EMPTY", "Phòng ban còn nhân sự, không thể ngừng hoạt động");
        }
        jdbc.update("UPDATE department SET code = ?, name = ?, description = ?, active = ? WHERE id = ?",
                r.code().trim(), r.name().trim(), r.description(), active, id);
        audit.success("UPDATE_DEPARTMENT", "department", id, r.decisionRef(), before,
                Map.of("code", r.code(), "name", r.name(), "active", active));
        return Sql.one(jdbc, "Không tìm thấy phòng ban", DEPT_SELECT + " WHERE d.id = ?", id);
    }

    // ---- Danh mục ----

    @GetMapping("/api/v1/job-titles")
    public List<Map<String, Object>> jobTitles() {
        Principals.current();
        return Sql.list(jdbc, "SELECT id, code, name, department_id, managerial, sales_owner_eligible, default_base_salary FROM job_title ORDER BY id");
    }

    @GetMapping("/api/v1/work-schedules")
    public List<Map<String, Object>> workSchedules() {
        Principals.current();
        List<Map<String, Object>> schedules = Sql.list(jdbc, "SELECT id, code, name, schedule_type, description FROM work_schedule ORDER BY id");
        for (Map<String, Object> s : schedules) {
            s.put("days", Sql.list(jdbc, """
                    SELECT day_of_week, morning_start, morning_end, afternoon_start, afternoon_end
                    FROM work_schedule_day WHERE schedule_id = ? ORDER BY day_of_week
                    """, s.get("id")));
        }
        return schedules;
    }

    @GetMapping("/api/v1/allowance-types")
    public List<Map<String, Object>> allowanceTypes() {
        Principals.current();
        return Sql.list(jdbc, "SELECT id, code, name, tax_class, insurance_included, included_in_ot_base, description FROM allowance_type ORDER BY id");
    }

    @GetMapping("/api/v1/leave-types")
    public List<Map<String, Object>> leaveTypes() {
        Principals.current();
        return Sql.list(jdbc, "SELECT id, code, name, paid, deduct_balance, requires_evidence FROM leave_type ORDER BY id");
    }

    @GetMapping("/api/v1/tax-brackets")
    public List<Map<String, Object>> taxBrackets() {
        Principals.current();
        return Sql.list(jdbc, "SELECT id, set_code, effective_from, effective_to, seq, lower_bound, upper_bound, rate, source_ref FROM tax_bracket ORDER BY set_code, effective_from, seq");
    }

    // ---- Ngày lễ ----

    @GetMapping("/api/v1/holidays")
    public List<Map<String, Object>> holidays(@RequestParam(required = false) Integer year) {
        Principals.current();
        int y = year == null ? LocalDate.now().getYear() : year;
        return Sql.list(jdbc, """
                SELECT id, holiday_date, name, paid, note FROM holiday
                WHERE holiday_date >= make_date(?, 1, 1) AND holiday_date <= make_date(?, 12, 31) ORDER BY holiday_date
                """, y, y);
    }

    @PostMapping("/api/v1/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createHoliday(@Valid @RequestBody HolidayRequest r) {
        Principals.requireRole("HR");
        guard.assertOpen(r.holidayDate());
        Long id = jdbc.queryForObject("INSERT INTO holiday(holiday_date, name, paid, note) VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, r.holidayDate(), r.name(), r.paid() == null || r.paid(), r.note());
        audit.success("CREATE_HOLIDAY", "holiday", id, r.name(), null, Map.of("date", r.holidayDate().toString()));
        return Sql.one(jdbc, "Không tìm thấy ngày lễ", "SELECT id, holiday_date, name, paid, note FROM holiday WHERE id = ?", id);
    }

    @DeleteMapping("/api/v1/holidays/{id}")
    @Transactional
    public ResponseEntity<Void> deleteHoliday(@PathVariable long id) {
        Principals.requireRole("HR");
        Map<String, Object> h = Sql.one(jdbc, "Không tìm thấy ngày lễ", "SELECT id, holiday_date, name FROM holiday WHERE id = ?", id);
        guard.assertOpen((LocalDate) h.get("holidayDate"));
        jdbc.update("DELETE FROM holiday WHERE id = ?", id);
        audit.success("DELETE_HOLIDAY", "holiday", id, null, h, null);
        return ResponseEntity.noContent().build();
    }
}
