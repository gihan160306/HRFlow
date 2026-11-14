package com.hrflow.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import com.hrflow.common.util.Sql;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

/**
 * Hồ sơ có hiệu lực theo thời gian của nhân viên: lương, phụ cấp, điều chuyển, người phụ thuộc,
 * bảo hiểm, thuế, lịch làm việc. Chỉ HR ghi; chỉ HR hoặc chính chủ đọc (dữ liệu nhạy cảm).
 * Mỗi thay đổi đóng bản ghi cũ ở ngày hôm trước và ghi audit; tháng đã chốt công bị chặn.
 */
@Service
public class EmployeeRecordsService {

    private static final Set<String> RESIDENCY = Set.of("RESIDENT", "NON_RESIDENT");
    private static final Set<String> TAX_METHOD = Set.of("PROGRESSIVE", "FLAT_10", "NON_RESIDENT_20", "MANUAL");
    private static final Set<String> STATUS = Set.of("CONFIRMED", "NEEDS_REVIEW");

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final PeriodGuard guard;
    private final EmployeeService employees;

    public EmployeeRecordsService(JdbcTemplate jdbc, AuditService audit, PeriodGuard guard, EmployeeService employees) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
        this.employees = employees;
    }

    /** HR hoặc chính chủ mới đọc được hồ sơ nhạy cảm; người khác nhận 404. */
    private void requireSelfOrHr(long employeeId) {
        AuthUser u = Principals.current();
        if (u.is("HR") || (u.employeeId() != null && u.employeeId() == employeeId)) {
            return;
        }
        throw new ResourceNotFoundException("Không tìm thấy nhân viên");
    }

    private void requireHrWriteTarget(long employeeId) {
        Principals.requireRole("HR");
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM employee WHERE id = ?", Integer.class, employeeId);
        if (n == null || n == 0) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
    }

    private void requireOpenEnding(String table, long employeeId, LocalDate from, String label) {
        Integer later = jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE employee_id = ? AND effective_from >= ?",
                Integer.class, employeeId, from);
        if (later != null && later > 0) {
            throw BusinessException.conflict("OVERLAPPING_PERIOD",
                    label + ": đã có bản ghi hiệu lực từ ngày này trở đi, hãy chọn ngày hiệu lực sau bản ghi gần nhất");
        }
        guard.assertOpen(from);
    }

    // ---------------- Lương ----------------

    public List<Map<String, Object>> salaries(long id) {
        requireSelfOrHr(id);
        return Sql.list(jdbc, """
                SELECT id, employee_id, contract_id, base_salary, effective_from, effective_to, reason, decision_ref, created_at
                FROM salary_record WHERE employee_id = ? ORDER BY effective_from DESC, id DESC
                """, id);
    }

    @Transactional
    public Map<String, Object> addSalary(long id, BigDecimal base, LocalDate from, String reason, String decisionRef) {
        requireHrWriteTarget(id);
        if (base.signum() < 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Lương cơ bản không được âm");
        }
        requireOpenEnding("salary_record", id, from, "Lương");
        checkMinimumWage(id, base);
        Long contractId = jdbc.query("SELECT id FROM contract WHERE employee_id = ? AND status IN ('ACTIVE','DRAFT') ORDER BY start_date DESC LIMIT 1",
                rs -> rs.next() ? rs.getLong(1) : null, id);
        jdbc.update("UPDATE salary_record SET effective_to = ? - 1 WHERE employee_id = ? AND effective_to IS NULL", from, id);
        Long sid = jdbc.queryForObject("""
                INSERT INTO salary_record(employee_id, contract_id, base_salary, effective_from, reason, decision_ref, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, id, contractId, base, from, reason, decisionRef, Principals.current().userId());
        audit.success("CHANGE_SALARY", "salary_record", sid, reason, null, Map.of("employeeId", id, "from", from.toString()));
        return Sql.one(jdbc, "Không tìm thấy lương", "SELECT id, employee_id, contract_id, base_salary, effective_from, effective_to, reason, decision_ref, created_at FROM salary_record WHERE id = ?", sid);
    }

    /** Lương toàn thời gian không thấp hơn lương tối thiểu tháng của vùng (payroll_rules Mục 3.2). Part-time/thực tập xét riêng. */
    void checkMinimumWage(long employeeId, BigDecimal base) {
        Map<String, Object> e = jdbc.queryForMap("SELECT employee_type, wage_region FROM employee WHERE id = ?", employeeId);
        String type = (String) e.get("employee_type");
        if (!"FULL_TIME".equals(type) && !"PROBATION".equals(type)) {
            return;
        }
        BigDecimal min = jdbc.query("""
                SELECT value FROM payroll_parameter WHERE code = 'MIN_WAGE_MONTHLY' AND region = ?
                ORDER BY effective_from DESC, param_version DESC LIMIT 1
                """, rs -> rs.next() ? rs.getBigDecimal(1) : null, e.get("wage_region"));
        if (min != null && base.compareTo(min) < 0) {
            throw BusinessException.unprocessable("BELOW_MIN_WAGE",
                    "Lương cơ bản thấp hơn lương tối thiểu vùng (" + min.toBigInteger() + ")");
        }
    }

    // ---------------- Phụ cấp ----------------

    private static final String ALLOWANCE_SELECT = """
            SELECT ea.id, ea.employee_id, ea.allowance_type_id, t.code AS allowance_code, t.name AS allowance_name,
                   ea.amount, ea.effective_from, ea.effective_to, ea.decision_ref, ea.status
            FROM employee_allowance ea JOIN allowance_type t ON t.id = ea.allowance_type_id
            """;

    public List<Map<String, Object>> allowances(long id) {
        requireSelfOrHr(id);
        return Sql.list(jdbc, ALLOWANCE_SELECT + " WHERE ea.employee_id = ? ORDER BY ea.effective_from DESC, ea.id DESC", id);
    }

    @Transactional
    public Map<String, Object> addAllowance(long id, long typeId, BigDecimal amount, LocalDate from, LocalDate to,
                                            String decisionRef) {
        requireHrWriteTarget(id);
        if (amount.signum() < 0 || (to != null && to.isBefore(from))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Số tiền hoặc khoảng hiệu lực không hợp lệ");
        }
        employees.requireExists("allowance_type", typeId, "Loại phụ cấp");
        guard.assertOpen(from);
        Integer overlap = jdbc.queryForObject("""
                SELECT COUNT(*) FROM employee_allowance WHERE employee_id = ? AND allowance_type_id = ?
                  AND effective_from <= COALESCE(?, DATE '9999-12-31') AND COALESCE(effective_to, DATE '9999-12-31') >= ?
                """, Integer.class, id, typeId, to, from);
        if (overlap != null && overlap > 0) {
            throw BusinessException.conflict("OVERLAPPING_PERIOD", "Phụ cấp cùng loại đã có hiệu lực trong khoảng này");
        }
        String type = jdbc.queryForObject("SELECT employee_type FROM employee WHERE id = ?", String.class, id);
        String status = "PROBATION".equals(type) || "INTERN".equals(type) ? "NEEDS_REVIEW" : "CONFIRMED";
        Long aid = jdbc.queryForObject("""
                INSERT INTO employee_allowance(employee_id, allowance_type_id, amount, effective_from, effective_to,
                                               decision_ref, status, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, id, typeId, amount, from, to, decisionRef, status, Principals.current().userId());
        audit.success("ADD_ALLOWANCE", "employee_allowance", aid, decisionRef, null, Map.of("employeeId", id, "typeId", typeId));
        return Sql.one(jdbc, "Không tìm thấy phụ cấp", ALLOWANCE_SELECT + " WHERE ea.id = ?", aid);
    }

    @Transactional
    public Map<String, Object> endAllowance(long allowanceId, LocalDate effectiveTo, String reason) {
        Principals.requireRole("HR");
        Map<String, Object> a = Sql.one(jdbc, "Không tìm thấy phụ cấp",
                "SELECT employee_id, effective_from, effective_to FROM employee_allowance WHERE id = ?", allowanceId);
        if (effectiveTo.isBefore((LocalDate) a.get("effectiveFrom"))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày kết thúc không được trước ngày bắt đầu");
        }
        guard.assertOpen(effectiveTo);
        jdbc.update("UPDATE employee_allowance SET effective_to = ? WHERE id = ?", effectiveTo, allowanceId);
        audit.success("END_ALLOWANCE", "employee_allowance", allowanceId, reason, a, Map.of("effectiveTo", effectiveTo.toString()));
        return Sql.one(jdbc, "Không tìm thấy phụ cấp", ALLOWANCE_SELECT + " WHERE ea.id = ?", allowanceId);
    }

    // ---------------- Điều chuyển ----------------

    private static final String ASSIGNMENT_SELECT = """
            SELECT a.id, a.department_id, d.name AS department_name, a.job_title_id, j.name AS job_title_name,
                   a.manager_id, m.full_name AS manager_name, a.effective_from, a.effective_to, a.reason, a.decision_ref
            FROM employee_assignment a JOIN department d ON d.id = a.department_id JOIN job_title j ON j.id = a.job_title_id
            LEFT JOIN employee m ON m.id = a.manager_id
            """;

    public List<Map<String, Object>> assignments(long id) {
        requireSelfOrHr(id);
        return Sql.list(jdbc, ASSIGNMENT_SELECT + " WHERE a.employee_id = ? ORDER BY a.effective_from DESC, a.id DESC", id);
    }

    @Transactional
    public Map<String, Object> addAssignment(long id, long departmentId, long jobTitleId, Long managerId, LocalDate from,
                                             String reason, String decisionRef) {
        requireHrWriteTarget(id);
        employees.requireExists("department", departmentId, "Phòng ban");
        employees.requireExists("job_title", jobTitleId, "Chức danh");
        if (managerId != null) {
            employees.requireExists("employee", managerId, "Quản lý trực tiếp");
            if (managerId == id) {
                throw BusinessException.unprocessable("VALIDATION_ERROR", "Nhân viên không thể tự quản lý chính mình");
            }
        }
        requireOpenEnding("employee_assignment", id, from, "Điều chuyển");
        jdbc.update("UPDATE employee_assignment SET effective_to = ? - 1 WHERE employee_id = ? AND effective_to IS NULL", from, id);
        Long aid = jdbc.queryForObject("""
                INSERT INTO employee_assignment(employee_id, department_id, job_title_id, manager_id, effective_from, reason,
                                                decision_ref, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, id, departmentId, jobTitleId, managerId, from, reason, decisionRef, Principals.current().userId());
        if (!from.isAfter(com.hrflow.common.util.Vn.today())) {
            jdbc.update("UPDATE employee SET department_id = ?, job_title_id = ?, manager_id = ?, version = version + 1 WHERE id = ?",
                    departmentId, jobTitleId, managerId, id);
        }
        audit.success("ASSIGN_EMPLOYEE", "employee_assignment", aid, reason, null, Map.of("employeeId", id, "from", from.toString()));
        return Sql.one(jdbc, "Không tìm thấy điều chuyển", ASSIGNMENT_SELECT + " WHERE a.id = ?", aid);
    }

    // ---------------- Người phụ thuộc ----------------

    public List<Map<String, Object>> dependents(long id) {
        requireSelfOrHr(id);
        return Sql.list(jdbc, """
                SELECT id, employee_id, full_name, relationship, valid_from, valid_to, doc_ref, verified
                FROM dependent WHERE employee_id = ? ORDER BY valid_from DESC, id
                """, id);
    }

    @Transactional
    public Map<String, Object> addDependent(long id, String name, String relationship, LocalDate from, LocalDate to,
                                            String docRef, Boolean verified) {
        requireHrWriteTarget(id);
        validateDependent(from, to, verified, docRef);
        guard.assertOpen(from);
        Long did = jdbc.queryForObject("""
                INSERT INTO dependent(employee_id, full_name, relationship, valid_from, valid_to, doc_ref, verified)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, id, name, relationship, from, to, docRef, verified != null && verified);
        audit.success("ADD_DEPENDENT", "dependent", did, null, null, Map.of("employeeId", id));
        return Sql.one(jdbc, "Không tìm thấy người phụ thuộc", "SELECT id, employee_id, full_name, relationship, valid_from, valid_to, doc_ref, verified FROM dependent WHERE id = ?", did);
    }

    @Transactional
    public Map<String, Object> updateDependent(long depId, String name, String relationship, LocalDate from, LocalDate to,
                                               String docRef, Boolean verified) {
        Principals.requireRole("HR");
        Map<String, Object> before = Sql.one(jdbc, "Không tìm thấy người phụ thuộc", "SELECT * FROM dependent WHERE id = ?", depId);
        validateDependent(from, to, verified, docRef);
        guard.assertOpen(from);
        jdbc.update("""
                UPDATE dependent SET full_name = ?, relationship = ?, valid_from = ?, valid_to = ?, doc_ref = ?, verified = ? WHERE id = ?
                """, name, relationship, from, to, docRef, verified != null && verified, depId);
        audit.success("UPDATE_DEPENDENT", "dependent", depId, null, Map.of("verified", before.get("verified")), Map.of("verified", verified != null && verified));
        return Sql.one(jdbc, "Không tìm thấy người phụ thuộc", "SELECT id, employee_id, full_name, relationship, valid_from, valid_to, doc_ref, verified FROM dependent WHERE id = ?", depId);
    }

    private static void validateDependent(LocalDate from, LocalDate to, Boolean verified, String docRef) {
        if (to != null && to.isBefore(from)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "validTo phải từ validFrom trở đi");
        }
        if (verified != null && verified && (docRef == null || docRef.isBlank())) {
            throw BusinessException.unprocessable("DEPENDENT_DOC_REQUIRED", "Người phụ thuộc đã xác minh phải có mã hồ sơ/giấy tờ");
        }
    }

    // ---------------- Bảo hiểm ----------------

    public List<Map<String, Object>> insuranceProfiles(long id) {
        requireSelfOrHr(id);
        return Sql.list(jdbc, """
                SELECT id, employee_id, effective_from, effective_to, bhxh, bhyt, bhtn, insurance_salary, status, verified_at, note
                FROM insurance_profile WHERE employee_id = ? ORDER BY effective_from DESC, id DESC
                """, id);
    }

    @Transactional
    public Map<String, Object> addInsurance(long id, LocalDate from, boolean bhxh, boolean bhyt, boolean bhtn,
                                            BigDecimal salary, String status, String note) {
        requireHrWriteTarget(id);
        if (!STATUS.contains(status)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Trạng thái hồ sơ không hợp lệ");
        }
        if (salary.signum() < 0 || ((bhxh || bhyt || bhtn) && salary.signum() == 0)) {
            throw BusinessException.unprocessable("INSURANCE_BASE_REQUIRED", "Tham gia bảo hiểm phải có mức đóng được HR xác nhận");
        }
        requireOpenEnding("insurance_profile", id, from, "Bảo hiểm");
        jdbc.update("UPDATE insurance_profile SET effective_to = ? - 1 WHERE employee_id = ? AND effective_to IS NULL", from, id);
        boolean confirmed = "CONFIRMED".equals(status);
        Long pid = jdbc.queryForObject("""
                INSERT INTO insurance_profile(employee_id, effective_from, bhxh, bhyt, bhtn, insurance_salary, status,
                                              verified_by, verified_at, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, id, from, bhxh, bhyt, bhtn, salary, status,
                confirmed ? Principals.current().userId() : null, confirmed ? java.sql.Timestamp.from(java.time.Instant.now()) : null, note);
        audit.success("ADD_INSURANCE_PROFILE", "insurance_profile", pid, note, null, Map.of("employeeId", id, "status", status));
        return Sql.one(jdbc, "Không tìm thấy hồ sơ bảo hiểm", "SELECT id, employee_id, effective_from, effective_to, bhxh, bhyt, bhtn, insurance_salary, status, verified_at, note FROM insurance_profile WHERE id = ?", pid);
    }

    // ---------------- Thuế ----------------

    public List<Map<String, Object>> taxProfiles(long id) {
        requireSelfOrHr(id);
        return Sql.list(jdbc, """
                SELECT id, employee_id, effective_from, effective_to, residency, method, tax_code, commitment_08,
                       commitment_doc_ref, status, verified_at, note
                FROM tax_profile WHERE employee_id = ? ORDER BY effective_from DESC, id DESC
                """, id);
    }

    @Transactional
    public Map<String, Object> addTaxProfile(long id, LocalDate from, String residency, String method, String taxCode,
                                             Boolean commitment08, String commitmentDocRef, String status, String note) {
        requireHrWriteTarget(id);
        if (!RESIDENCY.contains(residency) || !TAX_METHOD.contains(method) || !STATUS.contains(status)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Hồ sơ thuế không hợp lệ");
        }
        boolean commit = commitment08 != null && commitment08;
        if (commit && (commitmentDocRef == null || commitmentDocRef.isBlank())) {
            throw BusinessException.unprocessable("COMMITMENT_DOC_REQUIRED",
                    "tax_commitment_08 chỉ hợp lệ khi có hồ sơ cam kết (payroll_rules Mục 8.1)");
        }
        requireOpenEnding("tax_profile", id, from, "Hồ sơ thuế");
        jdbc.update("UPDATE tax_profile SET effective_to = ? - 1 WHERE employee_id = ? AND effective_to IS NULL", from, id);
        boolean confirmed = "CONFIRMED".equals(status);
        Long pid = jdbc.queryForObject("""
                INSERT INTO tax_profile(employee_id, effective_from, residency, method, tax_code, commitment_08,
                                        commitment_doc_ref, status, verified_by, verified_at, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, id, from, residency, method, taxCode, commit, commitmentDocRef, status,
                confirmed ? Principals.current().userId() : null, confirmed ? java.sql.Timestamp.from(java.time.Instant.now()) : null, note);
        audit.success("ADD_TAX_PROFILE", "tax_profile", pid, note, null, Map.of("employeeId", id, "method", method));
        return Sql.one(jdbc, "Không tìm thấy hồ sơ thuế", "SELECT id, employee_id, effective_from, effective_to, residency, method, tax_code, commitment_08, commitment_doc_ref, status, verified_at, note FROM tax_profile WHERE id = ?", pid);
    }

    // ---------------- Lịch làm việc cá nhân ----------------

    public List<Map<String, Object>> schedules(long id) {
        AuthUser u = Principals.current();
        if (!u.is("HR") && !employees.canSeeOperational(u, id)) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
        return Sql.list(jdbc, """
                SELECT es.id, es.employee_id, es.schedule_id, w.name AS schedule_name, es.effective_from, es.effective_to
                FROM employee_schedule es JOIN work_schedule w ON w.id = es.schedule_id
                WHERE es.employee_id = ? ORDER BY es.effective_from DESC, es.id DESC
                """, id);
    }

    @Transactional
    public Map<String, Object> addSchedule(long id, long scheduleId, LocalDate from, String reason) {
        requireHrWriteTarget(id);
        employees.requireExists("work_schedule", scheduleId, "Lịch làm việc");
        requireOpenEnding("employee_schedule", id, from, "Lịch làm việc");
        jdbc.update("UPDATE employee_schedule SET effective_to = ? - 1 WHERE employee_id = ? AND effective_to IS NULL", from, id);
        Long sid = jdbc.queryForObject("""
                INSERT INTO employee_schedule(employee_id, schedule_id, effective_from, created_by) VALUES (?, ?, ?, ?) RETURNING id
                """, Long.class, id, scheduleId, from, Principals.current().userId());
        audit.success("CHANGE_SCHEDULE", "employee_schedule", sid, reason, null, Map.of("employeeId", id, "scheduleId", scheduleId));
        return Sql.one(jdbc, "Không tìm thấy lịch", """
                SELECT es.id, es.employee_id, es.schedule_id, w.name AS schedule_name, es.effective_from, es.effective_to
                FROM employee_schedule es JOIN work_schedule w ON w.id = es.schedule_id WHERE es.id = ?
                """, sid);
    }
}
