package com.hrflow.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.response.ApiError.FieldDetail;
import com.hrflow.common.response.PageResponse;
import com.hrflow.common.util.Sql;
import com.hrflow.common.util.Vn;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/** Hồ sơ nhân viên (nhóm B). HR quản lý; Manager chỉ xem cơ bản của team; Admin chỉ định danh tối thiểu. */
@Service
public class EmployeeService {

    static final String SUMMARY_SELECT = """
            SELECT e.id, e.code, e.full_name, e.work_email, e.phone, e.department_id, d.name AS department_name,
                   e.job_title_id, j.code AS job_title_code, j.name AS job_title_name, e.manager_id,
                   m.full_name AS manager_name, e.employee_type, e.status, e.hire_date, e.termination_date,
                   EXISTS (SELECT 1 FROM user_account a WHERE a.employee_id = e.id) AS has_account
            FROM employee e JOIN department d ON d.id = e.department_id JOIN job_title j ON j.id = e.job_title_id
            LEFT JOIN employee m ON m.id = e.manager_id
            """;

    private static final Set<String> TYPES = Set.of("FULL_TIME", "PART_TIME", "PROBATION", "INTERN");
    private static final Set<String> REGIONS = Set.of("REGION_I", "REGION_II", "REGION_III", "REGION_IV");

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final PeriodGuard guard;

    public EmployeeService(JdbcTemplate jdbc, AuditService audit, PeriodGuard guard) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
    }

    // ---------------- phạm vi dữ liệu ----------------

    public boolean isTeamMember(long managerEmployeeId, long employeeId) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = ? AND manager_id = ?", Integer.class,
                employeeId, managerEmployeeId);
        return n != null && n > 0;
    }

    /** own | team | company scope: HR toàn công ty, Manager team + bản thân, Employee bản thân. Admin không có. */
    public boolean canSeeOperational(AuthUser u, long employeeId) {
        if (u.is("HR")) {
            return true;
        }
        if (u.employeeId() == null) {
            return false;
        }
        if (u.employeeId() == employeeId) {
            return true;
        }
        return u.is("MANAGER") && isTeamMember(u.employeeId(), employeeId);
    }

    /** Ném 404 (không lộ bản ghi) nếu ngoài phạm vi vận hành. */
    public void requireOperationalScope(long employeeId) {
        AuthUser u = Principals.current();
        if (!canSeeOperational(u, employeeId)) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
    }

    // ---------------- danh sách / chi tiết ----------------

    public PageResponse<Map<String, Object>> list(int page, int size, String q, Long departmentId, String status) {
        AuthUser u = Principals.current();
        if (u.is("EMPLOYEE")) {
            throw new AccessDeniedException("Nhân viên không xem danh sách nhân sự");
        }
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (u.is("MANAGER")) {
            where.append(" AND e.manager_id = ?");
            args.add(u.employeeId());
        }
        if (q != null && !q.isBlank()) {
            where.append(" AND (lower(e.full_name) LIKE ? OR lower(e.code) LIKE ? OR lower(e.work_email) LIKE ?)");
            String like = "%" + q.trim().toLowerCase() + "%";
            args.add(like);
            args.add(like);
            args.add(like);
        }
        if (departmentId != null) {
            where.append(" AND e.department_id = ?");
            args.add(departmentId);
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND e.status = ?");
            args.add(status);
        }
        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM employee e" + where, Long.class, args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(size);
        pageArgs.add((long) page * size);
        List<Map<String, Object>> rows = Sql.list(jdbc, SUMMARY_SELECT + where + " ORDER BY e.code LIMIT ? OFFSET ?",
                pageArgs.toArray());
        if (u.is("ADMIN")) {
            rows.forEach(EmployeeService::minimize);
        }
        return PageResponse.of(rows, page, size, total == null ? 0 : total);
    }

    public List<Map<String, Object>> options() {
        AuthUser u = Principals.requireRole("HR", "MANAGER", "ADMIN");
        String base = "SELECT e.id, e.code, e.full_name FROM employee e WHERE e.status <> 'TERMINATED'";
        if (u.is("MANAGER")) {
            return Sql.list(jdbc, base + " AND (e.manager_id = ? OR e.id = ?) ORDER BY e.code", u.employeeId(), u.employeeId());
        }
        return Sql.list(jdbc, base + " ORDER BY e.code");
    }

    private static void minimize(Map<String, Object> s) {
        s.put("phone", null);
        s.put("managerId", null);
        s.put("managerName", null);
        s.put("hireDate", null);
        s.put("terminationDate", null);
    }

    public Map<String, Object> detail(long id) {
        AuthUser u = Principals.current();
        Map<String, Object> summary = Sql.one(jdbc, "Không tìm thấy nhân viên", SUMMARY_SELECT + " WHERE e.id = ?", id);
        String visibility;
        if (u.is("HR") || (u.employeeId() != null && u.employeeId() == id)) {
            visibility = "FULL";
        } else if (u.is("MANAGER") && u.employeeId() != null && isTeamMember(u.employeeId(), id)) {
            visibility = "BASIC";
        } else if (u.is("ADMIN")) {
            visibility = "MINIMAL";
        } else {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        if ("MINIMAL".equals(visibility)) {
            minimize(summary);
        }
        out.put("summary", summary);
        out.put("visibility", visibility);
        if (!"MINIMAL".equals(visibility)) {
            Map<String, Object> d = Sql.one(jdbc, "Không tìm thấy nhân viên", """
                    SELECT gender, personal_email, address, work_location, wage_region, termination_reason, version,
                           date_of_birth, national_id, bank_name, bank_account_no FROM employee WHERE id = ?
                    """, id);
            if ("BASIC".equals(visibility)) {
                d.put("dateOfBirth", null);
                d.put("nationalId", null);
                d.put("bankName", null);
                d.put("bankAccountNo", null);
                d.put("personalEmail", null);
                d.put("address", null);
            }
            out.putAll(d);
        }
        return out;
    }

    public Map<String, Object> myProfile() {
        return detail(Principals.employeeId());
    }

    @Transactional
    public Map<String, Object> updateMyProfile(String phone, String personalEmail, String address) {
        long id = Principals.employeeId();
        Map<String, Object> before = Sql.one(jdbc, "Không tìm thấy nhân viên",
                "SELECT phone, personal_email, address FROM employee WHERE id = ?", id);
        jdbc.update("""
                UPDATE employee SET phone = COALESCE(?, phone), personal_email = COALESCE(?, personal_email),
                       address = COALESCE(?, address), version = version + 1 WHERE id = ?
                """, phone, personalEmail, address, id);
        audit.success("SELF_UPDATE_PROFILE", "employee", id, null, mask(before), Map.of("changed", "phone/personalEmail/address"));
        return detail(id);
    }

    // ---------------- tạo / sửa ----------------

    public record CreateCmd(String code, String fullName, String gender, LocalDate dateOfBirth, String phone,
                            String personalEmail, String workEmail, String address, String nationalId, String bankName,
                            String bankAccountNo, String employeeType, LocalDate hireDate, String workLocation,
                            String wageRegion, Long departmentId, Long jobTitleId, Long managerId, Long scheduleId) {
    }

    @Transactional
    public Map<String, Object> create(CreateCmd c) {
        AuthUser hr = Principals.requireRole("HR");
        if (!TYPES.contains(c.employeeType())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Loại nhân sự không hợp lệ");
        }
        if (c.wageRegion() == null || !REGIONS.contains(c.wageRegion())) {
            throw BusinessException.unprocessable("WAGE_REGION_REQUIRED",
                    "Phải xác định vùng lương tối thiểu theo nơi làm việc (OPEN-01), không mặc định theo tên thành phố");
        }
        requireExists("department", c.departmentId(), "Phòng ban");
        requireExists("job_title", c.jobTitleId(), "Chức danh");
        if (c.managerId() != null) {
            requireExists("employee", c.managerId(), "Quản lý trực tiếp");
        }
        if (c.scheduleId() != null) {
            requireExists("work_schedule", c.scheduleId(), "Lịch làm việc");
        }
        Long id = jdbc.queryForObject("""
                INSERT INTO employee(code, full_name, gender, date_of_birth, phone, personal_email, work_email, address,
                                     national_id, bank_name, bank_account_no, employee_type, status, hire_date,
                                     work_location, wage_region, department_id, job_title_id, manager_id)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'ONBOARDING', ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, c.code().trim(), c.fullName().trim(), c.gender(), c.dateOfBirth(), c.phone(),
                c.personalEmail(), c.workEmail().trim(), c.address(), blank(c.nationalId()), c.bankName(),
                c.bankAccountNo(), c.employeeType(), c.hireDate(), c.workLocation(), c.wageRegion(), c.departmentId(),
                c.jobTitleId(), c.managerId());
        jdbc.update("""
                INSERT INTO employee_assignment(employee_id, department_id, job_title_id, manager_id, effective_from,
                                                reason, decision_ref, created_by)
                VALUES (?, ?, ?, ?, ?, 'Tiếp nhận nhân sự', ?, ?)
                """, id, c.departmentId(), c.jobTitleId(), c.managerId(), c.hireDate(), "QD-TN-" + c.code().trim(), hr.userId());
        if (c.scheduleId() != null) {
            jdbc.update("INSERT INTO employee_schedule(employee_id, schedule_id, effective_from, created_by) VALUES (?, ?, ?, ?)",
                    id, c.scheduleId(), c.hireDate(), hr.userId());
        }
        audit.success("CREATE_EMPLOYEE", "employee", id, null, null, Map.of("code", c.code(), "type", c.employeeType()));
        return detail(id);
    }

    public record UpdateCmd(String fullName, String gender, LocalDate dateOfBirth, String phone, String personalEmail,
                            String address, String nationalId, String bankName, String bankAccountNo,
                            String employeeType, String workLocation, String wageRegion, String reason, Long version) {
    }

    @Transactional
    public Map<String, Object> update(long id, UpdateCmd c) {
        Principals.requireRole("HR");
        Map<String, Object> before = Sql.one(jdbc, "Không tìm thấy nhân viên", """
                SELECT full_name, gender, date_of_birth, phone, personal_email, address, national_id, bank_name,
                       bank_account_no, employee_type, work_location, wage_region, version FROM employee WHERE id = ?
                """, id);
        if (c.employeeType() != null && !TYPES.contains(c.employeeType())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Loại nhân sự không hợp lệ");
        }
        if (c.wageRegion() != null && !REGIONS.contains(c.wageRegion())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Vùng lương không hợp lệ");
        }
        long expected = c.version() == null ? ((Number) before.get("version")).longValue() : c.version();
        int n = jdbc.update("""
                UPDATE employee SET full_name = COALESCE(?, full_name), gender = COALESCE(?, gender),
                       date_of_birth = COALESCE(?, date_of_birth), phone = COALESCE(?, phone),
                       personal_email = COALESCE(?, personal_email), address = COALESCE(?, address),
                       national_id = COALESCE(?, national_id), bank_name = COALESCE(?, bank_name),
                       bank_account_no = COALESCE(?, bank_account_no), employee_type = COALESCE(?, employee_type),
                       work_location = COALESCE(?, work_location), wage_region = COALESCE(?, wage_region),
                       version = version + 1
                WHERE id = ? AND version = ?
                """, c.fullName(), c.gender(), c.dateOfBirth(), c.phone(), c.personalEmail(), c.address(),
                blank(c.nationalId()), c.bankName(), c.bankAccountNo(), c.employeeType(), c.workLocation(),
                c.wageRegion(), id, expected);
        if (n == 0) {
            throw BusinessException.conflict("VERSION_CONFLICT", "Hồ sơ đã được người khác cập nhật, hãy tải lại");
        }
        audit.success("UPDATE_EMPLOYEE", "employee", id, c.reason(), mask(before), mask(Sql.camel(jdbc.queryForMap("""
                SELECT full_name, gender, date_of_birth, phone, personal_email, address, national_id, bank_name,
                       bank_account_no, employee_type, work_location, wage_region FROM employee WHERE id = ?
                """, id))));
        return detail(id);
    }

    // ---------------- onboarding / nghỉ việc ----------------

    public Map<String, Object> onboardingCheck(long id) {
        Principals.requireRole("HR");
        requireExists("employee", id, "Nhân viên");
        List<String> missing = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (count("SELECT COUNT(*) FROM contract WHERE employee_id = ? AND status IN ('DRAFT', 'ACTIVE')", id) == 0) {
            missing.add("Thiếu hợp đồng lao động còn hiệu lực");
        }
        if (count("SELECT COUNT(*) FROM salary_record WHERE employee_id = ? AND effective_to IS NULL", id) == 0) {
            missing.add("Thiếu mức lương cơ bản có hiệu lực");
        }
        if (count("SELECT COUNT(*) FROM employee_schedule WHERE employee_id = ? AND effective_to IS NULL", id) == 0) {
            missing.add("Thiếu lịch làm việc cá nhân");
        }
        if (count("SELECT COUNT(*) FROM tax_profile WHERE employee_id = ?", id) == 0) {
            missing.add("Thiếu hồ sơ thuế (cư trú/phương pháp khấu trừ)");
        } else if (count("SELECT COUNT(*) FROM tax_profile WHERE employee_id = ? AND effective_to IS NULL AND status = 'NEEDS_REVIEW'", id) > 0) {
            warnings.add("Hồ sơ thuế đang NEEDS_REVIEW, cần HR xác nhận trước khi duyệt lương");
        }
        if (count("SELECT COUNT(*) FROM insurance_profile WHERE employee_id = ?", id) == 0) {
            missing.add("Thiếu hồ sơ bảo hiểm (đối tượng từng quỹ, mức đóng)");
        } else if (count("SELECT COUNT(*) FROM insurance_profile WHERE employee_id = ? AND effective_to IS NULL AND status = 'NEEDS_REVIEW'", id) > 0) {
            warnings.add("Hồ sơ bảo hiểm đang NEEDS_REVIEW, cần HR xác nhận trước khi duyệt lương");
        }
        if (count("SELECT COUNT(*) FROM approval_route WHERE requester_id = ?", id) == 0) {
            missing.add("Thiếu tuyến duyệt PTO/OT một cấp");
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("employeeId", id);
        out.put("ready", missing.isEmpty());
        out.put("missing", missing);
        out.put("warnings", warnings);
        return out;
    }

    @Transactional
    public Map<String, Object> activate(long id) {
        Principals.requireRole("HR");
        String status = jdbc.query("SELECT status FROM employee WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, id);
        if (status == null) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
        if (!"ONBOARDING".equals(status)) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ kích hoạt được nhân viên đang ONBOARDING");
        }
        @SuppressWarnings("unchecked")
        List<String> missing = (List<String>) onboardingCheck(id).get("missing");
        if (!missing.isEmpty()) {
            throw BusinessException.unprocessable("ONBOARDING_INCOMPLETE", "Hồ sơ onboarding còn thiếu dữ liệu",
                    missing.stream().map(m -> new FieldDetail("onboarding", m)).toList());
        }
        jdbc.update("UPDATE employee SET status = 'ACTIVE', version = version + 1 WHERE id = ?", id);
        jdbc.update("UPDATE contract SET status = 'ACTIVE' WHERE employee_id = ? AND status = 'DRAFT' AND start_date <= current_date", id);
        audit.success("ACTIVATE_EMPLOYEE", "employee", id, null, Map.of("status", "ONBOARDING"), Map.of("status", "ACTIVE"));
        return detail(id);
    }

    @Transactional
    public Map<String, Object> terminate(long id, LocalDate date, String reason) {
        AuthUser hr = Principals.requireRole("HR");
        Map<String, Object> e = Sql.one(jdbc, "Không tìm thấy nhân viên",
                "SELECT status, hire_date FROM employee WHERE id = ?", id);
        if ("TERMINATED".equals(e.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Nhân viên đã nghỉ việc");
        }
        if (date.isBefore((LocalDate) e.get("hireDate"))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày nghỉ việc không được trước ngày vào làm");
        }
        guard.assertOpen(date);
        jdbc.update("UPDATE employee SET status = 'TERMINATED', termination_date = ?, termination_reason = ?, version = version + 1 WHERE id = ?",
                date, reason, id);
        jdbc.update("""
                UPDATE contract SET status = 'TERMINATED', termination_date = ?, termination_reason = ?
                WHERE employee_id = ? AND status IN ('DRAFT', 'ACTIVE')
                """, date, reason, id);
        jdbc.update("UPDATE employee_allowance SET effective_to = ? WHERE employee_id = ? AND (effective_to IS NULL OR effective_to > ?)",
                date, id, date);
        // Đơn PTO/OT đang chờ duyệt không còn ý nghĩa sau ngày nghỉ.
        jdbc.update("UPDATE leave_request SET status = 'CANCELLED' WHERE employee_id = ? AND status IN ('DRAFT', 'PENDING') AND start_date > ?",
                id, date);
        jdbc.update("UPDATE user_account SET disable_at = ? WHERE employee_id = ?", date, id);
        audit.success("TERMINATE_EMPLOYEE", "employee", id, reason, Map.of("status", e.get("status")),
                Map.of("status", "TERMINATED", "terminationDate", date.toString(), "by", hr.userId()));
        return detail(id);
    }

    // ---------------- helpers ----------------

    void requireExists(String table, Long id, String label) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        if (n == null || n == 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", label + " không tồn tại");
        }
    }

    private int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Che số CCCD/tài khoản ngân hàng trước khi ghi audit (không ghi dữ liệu nhạy cảm nguyên văn). */
    private static Map<String, Object> mask(Map<String, Object> m) {
        Map<String, Object> out = new LinkedHashMap<>(m);
        for (String k : List.of("national_id", "nationalId", "bank_account_no", "bankAccountNo")) {
            if (out.get(k) != null) {
                out.put(k, "***");
            }
        }
        return out;
    }
}
