package com.hrflow.hr;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.util.Sql;
import com.hrflow.common.util.Vn;
import com.hrflow.security.AuthUser;
import com.hrflow.security.Principals;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Hợp đồng lao động: HR tạo/gia hạn/chấm dứt; nhân viên chỉ xem hợp đồng của mình. */
@RestController
@RequestMapping("/api/v1/contracts")
public class ContractController {

    private static final Set<String> TYPES = Set.of("INDEFINITE", "FIXED_TERM", "PROBATION", "INTERNSHIP", "PART_TIME");

    public record ContractCreateRequest(@NotNull Long employeeId, @NotBlank String contractNo,
                                        @NotBlank String contractType, @NotNull LocalDate startDate, LocalDate endDate,
                                        LocalDate signedDate, @NotNull BigDecimal baseSalary, String note,
                                        String decisionRef) {
    }

    public record TerminateRequest(@NotNull LocalDate terminationDate, @NotBlank String reason) {
    }

    public record RenewRequest(@NotBlank String contractNo, String contractType, LocalDate endDate,
                               BigDecimal baseSalary, @NotBlank String reason) {
    }

    private static final String SELECT = """
            SELECT c.id, c.employee_id, e.code AS employee_code, e.full_name AS employee_name, c.contract_no,
                   c.contract_type, c.start_date, c.end_date, c.signed_date, c.status, c.previous_contract_id,
                   c.termination_date, c.termination_reason, c.note,
                   CASE WHEN c.status = 'ACTIVE' AND c.end_date IS NOT NULL THEN (c.end_date - current_date) END AS days_to_expire
            FROM contract c JOIN employee e ON e.id = c.employee_id
            """;

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final PeriodGuard guard;

    public ContractController(JdbcTemplate jdbc, AuditService audit, PeriodGuard guard) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
    }

    private void expireOverdue() {
        jdbc.update("UPDATE contract SET status = 'EXPIRED' WHERE status = 'ACTIVE' AND end_date IS NOT NULL AND end_date < current_date");
    }

    @GetMapping
    public List<Map<String, Object>> list(@RequestParam(required = false) Long employeeId,
                                          @RequestParam(required = false) String status,
                                          @RequestParam(required = false) Integer expiringDays) {
        AuthUser u = Principals.current();
        if (u.is("ADMIN")) {
            throw new AccessDeniedException("Admin không truy cập hợp đồng lao động");
        }
        expireOverdue();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (u.is("HR")) {
            if (employeeId != null) {
                where.append(" AND c.employee_id = ?");
                args.add(employeeId);
            }
        } else {
            where.append(" AND c.employee_id = ?");
            args.add(Principals.employeeId());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND c.status = ?");
            args.add(status);
        }
        if (expiringDays != null) {
            where.append(" AND c.status = 'ACTIVE' AND c.end_date IS NOT NULL AND c.end_date <= current_date + ?");
            args.add(expiringDays);
        }
        return Sql.list(jdbc, SELECT + where + " ORDER BY c.start_date DESC, c.id DESC", args.toArray());
    }

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable long id) {
        AuthUser u = Principals.current();
        expireOverdue();
        Map<String, Object> c = Sql.one(jdbc, "Không tìm thấy hợp đồng", SELECT + " WHERE c.id = ?", id);
        if (!u.is("HR") && (u.employeeId() == null || ((Number) c.get("employeeId")).longValue() != u.employeeId())) {
            throw new ResourceNotFoundException("Không tìm thấy hợp đồng");
        }
        return c;
    }

    /**
     * Dữ liệu đầy đủ để dựng văn bản hợp đồng (PDF): hợp đồng, nhân viên, phòng ban/chức danh,
     * lương và phụ cấp hiệu lực tại ngày bắt đầu, người đại diện công ty. Cùng phạm vi quyền với {@link #get}.
     */
    @GetMapping("/{id}/document")
    public Map<String, Object> document(@PathVariable long id) {
        Map<String, Object> c = get(id);
        long employeeId = ((Number) c.get("employeeId")).longValue();
        LocalDate start = (LocalDate) c.get("startDate");

        Map<String, Object> doc = new java.util.LinkedHashMap<>(c);
        doc.put("employee", Sql.one(jdbc, "Không tìm thấy nhân viên", """
                SELECT e.code, e.full_name, e.gender, e.date_of_birth, e.national_id, e.address, e.phone,
                       e.personal_email, e.work_email, e.employee_type, e.hire_date, e.work_location,
                       d.code AS department_code, d.name AS department_name, d.description AS department_description,
                       j.code AS job_title_code, j.name AS job_title_name, j.managerial,
                       m.full_name AS manager_name, mj.name AS manager_title
                FROM employee e
                JOIN department d ON d.id = e.department_id
                JOIN job_title j ON j.id = e.job_title_id
                LEFT JOIN employee m ON m.id = e.manager_id
                LEFT JOIN job_title mj ON mj.id = m.job_title_id
                WHERE e.id = ?
                """, employeeId));

        List<Map<String, Object>> salary = Sql.list(jdbc, """
                SELECT base_salary FROM salary_record WHERE employee_id = ?
                ORDER BY (contract_id = ?) DESC NULLS LAST,
                         (effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?)) DESC, effective_from DESC
                LIMIT 1
                """, employeeId, id, start, start);
        doc.put("baseSalary", salary.isEmpty() ? null : salary.get(0).get("baseSalary"));

        LocalDate asOf = start.isAfter(Vn.today()) ? start : Vn.today();
        doc.put("allowances", Sql.list(jdbc, """
                SELECT t.code, t.name, a.amount FROM employee_allowance a JOIN allowance_type t ON t.id = a.allowance_type_id
                WHERE a.employee_id = ? AND a.effective_from <= ? AND (a.effective_to IS NULL OR a.effective_to >= ?)
                ORDER BY t.id
                """, employeeId, asOf, asOf));

        List<Map<String, Object>> rep = Sql.list(jdbc, """
                SELECT e.full_name, e.gender, 'Giám đốc' AS title FROM employee e JOIN job_title j ON j.id = e.job_title_id
                WHERE j.code = 'DIRECTOR' AND e.status <> 'TERMINATED' ORDER BY e.id LIMIT 1
                """);
        doc.put("representative", rep.isEmpty() ? null : rep.get(0));
        return doc;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> create(@Valid @RequestBody ContractCreateRequest r) {
        AuthUser hr = Principals.requireRole("HR");
        validate(r.contractType(), r.startDate(), r.endDate(), r.baseSalary());
        guard.assertOpen(r.startDate());
        String empStatus = jdbc.query("SELECT status FROM employee WHERE id = ?", rs -> rs.next() ? rs.getString(1) : null, r.employeeId());
        if (empStatus == null) {
            throw new ResourceNotFoundException("Không tìm thấy nhân viên");
        }
        if ("TERMINATED".equals(empStatus)) {
            throw BusinessException.conflict("INVALID_STATE", "Nhân viên đã nghỉ việc");
        }
        assertNoOverlap(r.employeeId(), r.startDate(), r.endDate(), null);
        String status = r.startDate().isAfter(Vn.today()) || "ONBOARDING".equals(empStatus) ? "DRAFT" : "ACTIVE";
        Long id = jdbc.queryForObject("""
                INSERT INTO contract(employee_id, contract_no, contract_type, start_date, end_date, signed_date, status, note, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, r.employeeId(), r.contractNo().trim(), r.contractType(), r.startDate(), r.endDate(),
                r.signedDate(), status, r.note(), hr.userId());
        applySalary(r.employeeId(), id, r.baseSalary(), r.startDate(), "Lương theo hợp đồng " + r.contractNo(), r.decisionRef(), hr.userId());
        audit.success("CREATE_CONTRACT", "contract", id, r.decisionRef(), null, Map.of("employeeId", r.employeeId(), "no", r.contractNo()));
        return Sql.one(jdbc, "Không tìm thấy hợp đồng", SELECT + " WHERE c.id = ?", id);
    }

    @PostMapping("/{id}/terminate")
    @Transactional
    public Map<String, Object> terminate(@PathVariable long id, @Valid @RequestBody TerminateRequest r) {
        Principals.requireRole("HR");
        Map<String, Object> c = Sql.one(jdbc, "Không tìm thấy hợp đồng", SELECT + " WHERE c.id = ?", id);
        if (!Set.of("ACTIVE", "DRAFT").contains(c.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ chấm dứt được hợp đồng ACTIVE/DRAFT");
        }
        if (r.terminationDate().isBefore((LocalDate) c.get("startDate"))) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày chấm dứt không được trước ngày bắt đầu");
        }
        guard.assertOpen(r.terminationDate());
        jdbc.update("UPDATE contract SET status = 'TERMINATED', termination_date = ?, termination_reason = ?, version = version + 1 WHERE id = ?",
                r.terminationDate(), r.reason(), id);
        audit.success("TERMINATE_CONTRACT", "contract", id, r.reason(), Map.of("status", c.get("status")), Map.of("status", "TERMINATED"));
        return Sql.one(jdbc, "Không tìm thấy hợp đồng", SELECT + " WHERE c.id = ?", id);
    }

    @PostMapping("/{id}/renew")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> renew(@PathVariable long id, @Valid @RequestBody RenewRequest r) {
        AuthUser hr = Principals.requireRole("HR");
        Map<String, Object> old = Sql.one(jdbc, "Không tìm thấy hợp đồng", SELECT + " WHERE c.id = ?", id);
        if (!Set.of("ACTIVE", "EXPIRED").contains(old.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ gia hạn được hợp đồng ACTIVE/EXPIRED");
        }
        long employeeId = ((Number) old.get("employeeId")).longValue();
        LocalDate oldEnd = (LocalDate) old.get("endDate");
        LocalDate start = oldEnd == null ? Vn.today() : oldEnd.plusDays(1);
        String type = r.contractType() == null ? (String) old.get("contractType") : r.contractType();
        validate(type, start, r.endDate(), r.baseSalary() == null ? BigDecimal.ZERO : r.baseSalary());
        guard.assertOpen(start);
        jdbc.update("UPDATE contract SET status = 'EXPIRED' WHERE id = ? AND status = 'ACTIVE'", id);
        assertNoOverlap(employeeId, start, r.endDate(), id);
        String status = start.isAfter(Vn.today()) ? "DRAFT" : "ACTIVE";
        Long nid = jdbc.queryForObject("""
                INSERT INTO contract(employee_id, contract_no, contract_type, start_date, end_date, signed_date, status,
                                     previous_contract_id, note, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, employeeId, r.contractNo().trim(), type, start, r.endDate(), Vn.today(), status, id,
                r.reason(), hr.userId());
        if (r.baseSalary() != null) {
            applySalary(employeeId, nid, r.baseSalary(), start, r.reason(), r.contractNo(), hr.userId());
        }
        audit.success("RENEW_CONTRACT", "contract", nid, r.reason(), null, Map.of("previous", id));
        return Sql.one(jdbc, "Không tìm thấy hợp đồng", SELECT + " WHERE c.id = ?", nid);
    }

    private void validate(String type, LocalDate start, LocalDate end, BigDecimal salary) {
        if (!TYPES.contains(type)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Loại hợp đồng không hợp lệ");
        }
        if (end != null && end.isBefore(start)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Ngày kết thúc phải từ ngày bắt đầu trở đi");
        }
        if (!"INDEFINITE".equals(type) && end == null) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Hợp đồng có thời hạn phải có ngày kết thúc");
        }
        if ("INDEFINITE".equals(type) && end != null) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Hợp đồng không xác định thời hạn không có ngày kết thúc");
        }
        if (salary.signum() < 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Lương cơ bản không được âm");
        }
    }

    private void assertNoOverlap(long employeeId, LocalDate start, LocalDate end, Long ignoreId) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM contract WHERE employee_id = ? AND status IN ('DRAFT', 'ACTIVE')
                  AND start_date <= COALESCE(?, DATE '9999-12-31') AND COALESCE(end_date, DATE '9999-12-31') >= ?
                  AND (?::bigint IS NULL OR id <> ?)
                """, Integer.class, employeeId, end, start, ignoreId, ignoreId);
        if (n != null && n > 0) {
            throw BusinessException.conflict("OVERLAPPING_PERIOD", "Nhân viên đã có hợp đồng còn hiệu lực trong khoảng này");
        }
    }

    private void applySalary(long employeeId, long contractId, BigDecimal salary, LocalDate from, String reason,
                             String ref, Long userId) {
        Integer later = jdbc.queryForObject("SELECT COUNT(*) FROM salary_record WHERE employee_id = ? AND effective_from >= ?",
                Integer.class, employeeId, from);
        if (later != null && later > 0) {
            throw BusinessException.conflict("OVERLAPPING_PERIOD", "Đã có mức lương hiệu lực từ ngày này trở đi");
        }
        jdbc.update("UPDATE salary_record SET effective_to = ? - 1 WHERE employee_id = ? AND effective_to IS NULL", from, employeeId);
        jdbc.update("""
                INSERT INTO salary_record(employee_id, contract_id, base_salary, effective_from, reason, decision_ref, created_by)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, employeeId, contractId, salary, from, reason, ref, userId);
    }
}
