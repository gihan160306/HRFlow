package com.hrflow.income;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.hrflow.common.PeriodGuard;
import com.hrflow.common.audit.AuditService;
import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.util.Sql;
import com.hrflow.hr.EmployeeService;
import com.hrflow.payroll.ParameterService;
import com.hrflow.security.AuthUser;
import com.hrflow.security.PayrollAccess;
import com.hrflow.security.Principals;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Thu nhập khác/khấu trừ (income_item) và doanh thu Sales (sales_revenue) – nguồn vào payroll, có mã nguồn duy nhất. */
@RestController
public class IncomeController {

    private static final Set<String> TYPES = Set.of("BONUS", "OTHER_ADDITION", "DEDUCTION", "COMPENSATION", "ADVANCE_RECOVERY", "REIMBURSEMENT");
    private static final Set<String> TAX = Set.of("TAXABLE", "NON_TAXABLE", "EXEMPT", "NEEDS_REVIEW");

    public record IncomeItemRequest(@NotNull Long employeeId, @NotNull Integer periodYear, @NotNull Integer periodMonth,
                                    @NotBlank String itemType, @NotBlank String name, @NotNull BigDecimal amount,
                                    String taxClass, @NotBlank String sourceRef, String evidence, String decisionRef, String note) {
    }

    public record ClassifyRequest(String taxClass, String note) {
    }

    public record NoteBody(String note) {
    }

    public record SalesRequest(Long ownerEmployeeId, @NotNull Integer periodYear, @NotNull Integer periodMonth,
                               @NotBlank String contractCode, @NotBlank String receiptCode, @NotNull BigDecimal paymentAmount,
                               @NotNull BigDecimal eligibleRevenue, LocalDate paymentDate, LocalDate acceptanceDate,
                               BigDecimal refundAmount, String note) {
    }

    private static final String ITEM_SELECT = """
            SELECT i.id, i.employee_id, e.code AS employee_code, e.full_name AS employee_name, i.period_year, i.period_month,
                   i.item_type, i.name, i.amount, i.tax_class, i.source_ref, i.evidence, i.decision_ref, i.status,
                   COALESCE(ce.full_name, ca.email) AS created_by_name, COALESCE(fe.full_name, fa.email) AS confirmed_by_name,
                   i.confirmed_at, i.note, i.created_by
            FROM income_item i JOIN employee e ON e.id = i.employee_id JOIN user_account ca ON ca.id = i.created_by
            LEFT JOIN employee ce ON ce.id = ca.employee_id LEFT JOIN user_account fa ON fa.id = i.confirmed_by
            LEFT JOIN employee fe ON fe.id = fa.employee_id
            """;

    private final JdbcTemplate jdbc;
    private final AuditService audit;
    private final PeriodGuard guard;
    private final PayrollAccess access;
    private final EmployeeService employees;
    private final ParameterService params;

    public IncomeController(JdbcTemplate jdbc, AuditService audit, PeriodGuard guard, PayrollAccess access,
                            EmployeeService employees, ParameterService params) {
        this.jdbc = jdbc;
        this.audit = audit;
        this.guard = guard;
        this.access = access;
        this.employees = employees;
        this.params = params;
    }

    private Map<String, Object> item(long id) {
        Map<String, Object> m = Sql.one(jdbc, "Không tìm thấy khoản", ITEM_SELECT + " WHERE i.id = ?", id);
        return decorate(m);
    }

    private Map<String, Object> decorate(Map<String, Object> m) {
        AuthUser u = Principals.current();
        m.put("canConfirm", "DRAFT".equals(m.get("status")) && canConfirmItem(u, m));
        return m;
    }

    private boolean canConfirmItem(AuthUser u, Map<String, Object> m) {
        long emp = ((Number) m.get("employeeId")).longValue();
        return ((Number) m.get("createdBy")).longValue() != u.userId() && (u.employeeId() == null || u.employeeId() != emp)
                && (access.has("PREPARE", null) || access.has("APPROVE", null));
    }

    @GetMapping("/api/v1/income-items")
    public List<Map<String, Object>> items(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month,
                                           @RequestParam(required = false) Long employeeId) {
        AuthUser u = Principals.current();
        if (u.employeeId() == null) {
            throw new AccessDeniedException("Admin không truy cập khoản thu nhập");
        }
        boolean privileged = u.is("HR") || access.hasAny(null, "VIEW", "PREPARE", "APPROVE");
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        if (!privileged) {
            where.append(" AND i.employee_id = ?");
            args.add(u.employeeId());
        } else if (employeeId != null) {
            where.append(" AND i.employee_id = ?");
            args.add(employeeId);
        }
        if (year != null) {
            where.append(" AND i.period_year = ?");
            args.add(year);
        }
        if (month != null) {
            where.append(" AND i.period_month = ?");
            args.add(month);
        }
        return Sql.list(jdbc, ITEM_SELECT + where + " ORDER BY i.id DESC LIMIT 500", args.toArray()).stream().map(this::decorate).toList();
    }

    @PostMapping("/api/v1/income-items")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createItem(@Valid @RequestBody IncomeItemRequest r) {
        AuthUser hr = Principals.requireRole("HR");
        if (!TYPES.contains(r.itemType())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Loại khoản không hợp lệ");
        }
        if (r.amount().signum() <= 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Số tiền phải lớn hơn 0");
        }
        if (r.periodMonth() < 1 || r.periodMonth() > 12) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Tháng không hợp lệ");
        }
        String tax = r.taxClass() == null ? "NEEDS_REVIEW" : r.taxClass();
        if (!TAX.contains(tax)) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Phân loại thuế không hợp lệ");
        }
        // Khấu trừ/bồi thường/thu hồi tạm ứng phải có căn cứ và chứng từ (payroll_rules Mục 9).
        if (Set.of("DEDUCTION", "COMPENSATION", "ADVANCE_RECOVERY").contains(r.itemType())
                && (r.evidence() == null || r.evidence().isBlank() || r.decisionRef() == null || r.decisionRef().isBlank())) {
            throw BusinessException.unprocessable("DEDUCTION_BASIS_REQUIRED", "Khấu trừ/thu hồi phải có chứng từ và căn cứ (quyết định)");
        }
        LocalDate start = LocalDate.of(r.periodYear(), r.periodMonth(), 1);
        guard.assertOpen(start, start.plusMonths(1).minusDays(1));
        Long id = jdbc.queryForObject("""
                INSERT INTO income_item(employee_id, period_year, period_month, item_type, name, amount, tax_class, source_ref,
                       evidence, decision_ref, status, created_by, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?) RETURNING id
                """, Long.class, r.employeeId(), r.periodYear(), r.periodMonth(), r.itemType(), r.name(), r.amount(), tax,
                r.sourceRef().trim(), r.evidence(), r.decisionRef(), hr.userId(), r.note());
        audit.success("CREATE_INCOME_ITEM", "income_item", id, r.note(), null, Map.of("type", r.itemType(), "employeeId", r.employeeId()));
        return item(id);
    }

    @PostMapping("/api/v1/income-items/{id}/classify")
    @Transactional
    public Map<String, Object> classify(@PathVariable long id, @RequestBody ClassifyRequest r) {
        Principals.requireRole("HR");
        if (r.taxClass() == null || !TAX.contains(r.taxClass())) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Phân loại thuế không hợp lệ");
        }
        Map<String, Object> m = item(id);
        if (!"DRAFT".equals(m.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Chỉ phân loại được khoản DRAFT");
        }
        jdbc.update("UPDATE income_item SET tax_class = ?, note = COALESCE(?, note) WHERE id = ?", r.taxClass(), r.note(), id);
        audit.success("CLASSIFY_INCOME_ITEM", "income_item", id, r.note(), Map.of("taxClass", m.get("taxClass")), Map.of("taxClass", r.taxClass()));
        return item(id);
    }

    @PostMapping("/api/v1/income-items/{id}/confirm")
    @Transactional
    public Map<String, Object> confirmItem(@PathVariable long id, @RequestBody(required = false) NoteBody b) {
        AuthUser u = Principals.current();
        Map<String, Object> m = item(id);
        long emp = ((Number) m.get("employeeId")).longValue();
        if (((Number) m.get("createdBy")).longValue() == u.userId() || (u.employeeId() != null && u.employeeId() == emp)) {
            audit.denied("SELF_APPROVAL", "income_item", id, "Người tạo/người hưởng tự xác nhận khoản");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Người xác nhận phải khác người tạo và người hưởng");
        }
        access.requireAny(null, "PREPARE", "APPROVE");
        if (!"DRAFT".equals(m.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Khoản không ở trạng thái DRAFT");
        }
        if ("NEEDS_REVIEW".equals(m.get("taxClass"))) {
            throw BusinessException.unprocessable("TAX_CLASS_REQUIRED", "Phải phân loại thuế trước khi xác nhận");
        }
        LocalDate start = LocalDate.of(((Number) m.get("periodYear")).intValue(), ((Number) m.get("periodMonth")).intValue(), 1);
        guard.assertOpen(start, start.plusMonths(1).minusDays(1));
        jdbc.update("UPDATE income_item SET status = 'CONFIRMED', confirmed_by = ?, confirmed_at = now(), note = COALESCE(?, note) WHERE id = ?",
                u.userId(), b == null ? null : b.note(), id);
        audit.success("CONFIRM_INCOME_ITEM", "income_item", id, b == null ? null : b.note(), null, null);
        return item(id);
    }

    @PostMapping("/api/v1/income-items/{id}/reject")
    @Transactional
    public Map<String, Object> rejectItem(@PathVariable long id, @RequestBody(required = false) NoteBody b) {
        AuthUser u = Principals.current();
        access.requireAny(null, "PREPARE", "APPROVE");
        Map<String, Object> m = item(id);
        if (!"DRAFT".equals(m.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Khoản không ở trạng thái DRAFT");
        }
        jdbc.update("UPDATE income_item SET status = 'REJECTED', confirmed_by = ?, confirmed_at = now(), note = COALESCE(?, note) WHERE id = ?",
                u.userId(), b == null ? null : b.note(), id);
        audit.success("REJECT_INCOME_ITEM", "income_item", id, b == null ? null : b.note(), null, null);
        return item(id);
    }

    // ---------------- doanh thu Sales ----------------

    private static final String SALES_SELECT = """
            SELECT s.id, s.period_year, s.period_month, s.owner_employee_id, o.code AS owner_code, o.full_name AS owner_name,
                   s.contract_code, s.receipt_code, s.payment_amount, s.eligible_revenue, s.payment_date, s.acceptance_date,
                   s.refund_amount, s.status, oc.full_name AS owner_confirmed_by_name, s.owner_confirmed_at,
                   fc.full_name AS finance_confirmed_by_name, s.finance_confirmed_at, s.note
            FROM sales_revenue s JOIN employee o ON o.id = s.owner_employee_id
            LEFT JOIN employee oc ON oc.id = s.owner_confirmed_by LEFT JOIN employee fc ON fc.id = s.finance_confirmed_by
            """;

    private boolean isTitle(long employeeId, String code) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM employee e JOIN job_title j ON j.id = e.job_title_id WHERE e.id = ? AND j.code = ?",
                Integer.class, employeeId, code);
        return n != null && n > 0;
    }

    private Map<String, Object> sale(long id) {
        Map<String, Object> m = Sql.one(jdbc, "Không tìm thấy khoản doanh thu", SALES_SELECT + " WHERE s.id = ?", id);
        return decorateSale(m);
    }

    private Map<String, Object> decorateSale(Map<String, Object> m) {
        AuthUser u = Principals.current();
        Long me = u.employeeId();
        long owner = ((Number) m.get("ownerEmployeeId")).longValue();
        boolean salesMgr = me != null && isTitle(me, "SALES_MANAGER") && employees.isTeamMember(me, owner);
        boolean fin = me != null && isTitle(me, "FIN_MANAGER");
        m.put("canConfirmOwner", "DRAFT".equals(m.get("status")) && salesMgr && me != owner);
        m.put("canConfirmPayment", "OWNER_CONFIRMED".equals(m.get("status")) && fin);
        return m;
    }

    @GetMapping("/api/v1/sales-revenues")
    public List<Map<String, Object>> sales(@RequestParam(required = false) Integer year, @RequestParam(required = false) Integer month) {
        AuthUser u = Principals.current();
        if (u.employeeId() == null) {
            throw new AccessDeniedException("Admin không truy cập doanh thu Sales");
        }
        long me = u.employeeId();
        List<Object> args = new ArrayList<>();
        StringBuilder where = new StringBuilder(" WHERE ");
        if (isTitle(me, "FIN_MANAGER")) {
            where.append("1=1");
        } else if (isTitle(me, "SALES_MANAGER")) {
            where.append("(s.owner_employee_id = ? OR o.manager_id = ?)");
            args.add(me);
            args.add(me);
        } else if (u.is("HR")) {
            where.append("s.status = 'CONFIRMED'"); // HR chỉ dùng khoản đã được xác nhận đủ (permission_matrix Mục 3.3)
        } else {
            where.append("s.owner_employee_id = ?");
            args.add(me);
        }
        if (year != null) {
            where.append(" AND s.period_year = ?");
            args.add(year);
        }
        if (month != null) {
            where.append(" AND s.period_month = ?");
            args.add(month);
        }
        return Sql.list(jdbc, SALES_SELECT + where + " ORDER BY s.id DESC LIMIT 500", args.toArray()).stream().map(this::decorateSale).toList();
    }

    @PostMapping("/api/v1/sales-revenues")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Map<String, Object> createSale(@Valid @RequestBody SalesRequest r) {
        AuthUser u = Principals.current();
        long me = Principals.employeeId();
        boolean mgr = isTitle(me, "SALES_MANAGER");
        long owner = r.ownerEmployeeId() == null || !mgr ? me : r.ownerEmployeeId();
        Boolean eligible = jdbc.query("SELECT j.sales_owner_eligible FROM employee e JOIN job_title j ON j.id = e.job_title_id WHERE e.id = ?",
                rs -> rs.next() ? rs.getBoolean(1) : null, owner);
        if (eligible == null || !eligible) {
            throw BusinessException.unprocessable("NOT_SALES_OWNER", "Chỉ Sales Executive được giao sở hữu doanh thu mới hưởng hoa hồng/KPI");
        }
        if (mgr && !employees.isTeamMember(me, owner) && owner != me) {
            throw BusinessException.forbidden("OUT_OF_TEAM", "Chỉ ghi doanh thu cho Sales thuộc team");
        }
        if (r.eligibleRevenue().signum() < 0 || r.eligibleRevenue().compareTo(r.paymentAmount()) > 0) {
            throw BusinessException.unprocessable("VALIDATION_ERROR", "Doanh thu đủ điều kiện không được âm hoặc vượt số tiền thực nhận");
        }
        LocalDate start = LocalDate.of(r.periodYear(), r.periodMonth(), 1);
        guard.assertOpen(start, start.plusMonths(1).minusDays(1));
        Long id = jdbc.queryForObject("""
                INSERT INTO sales_revenue(period_year, period_month, owner_employee_id, contract_code, receipt_code, payment_amount,
                       eligible_revenue, payment_date, acceptance_date, refund_amount, status, created_by, note)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'DRAFT', ?, ?) RETURNING id
                """, Long.class, r.periodYear(), r.periodMonth(), owner, r.contractCode(), r.receiptCode().trim(), r.paymentAmount(),
                r.eligibleRevenue(), r.paymentDate(), r.acceptanceDate(), r.refundAmount() == null ? BigDecimal.ZERO : r.refundAmount(),
                u.userId(), r.note());
        audit.success("CREATE_SALES_REVENUE", "sales_revenue", id, r.note(), null, Map.of("receipt", r.receiptCode()));
        return sale(id);
    }

    @PostMapping("/api/v1/sales-revenues/{id}/confirm-owner")
    @Transactional
    public Map<String, Object> confirmOwner(@PathVariable long id) {
        long me = Principals.employeeId();
        Map<String, Object> m = sale(id);
        long owner = ((Number) m.get("ownerEmployeeId")).longValue();
        if (me == owner) {
            audit.denied("SELF_APPROVAL", "sales_revenue", id, "Sales owner tự xác nhận");
            throw BusinessException.forbidden("SELF_APPROVAL_FORBIDDEN", "Không tự xác nhận doanh thu của mình");
        }
        if (!isTitle(me, "SALES_MANAGER") || !employees.isTeamMember(me, owner)) {
            throw new AccessDeniedException("Chỉ Sales Manager của team xác nhận Sales owner");
        }
        if (!"DRAFT".equals(m.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Khoản không ở trạng thái DRAFT");
        }
        if (m.get("acceptanceDate") == null) {
            throw BusinessException.unprocessable("ACCEPTANCE_REQUIRED", "Doanh thu phải có ngày nghiệm thu");
        }
        jdbc.update("UPDATE sales_revenue SET status = 'OWNER_CONFIRMED', owner_confirmed_by = ?, owner_confirmed_at = now() WHERE id = ?", me, id);
        audit.success("CONFIRM_SALES_OWNER", "sales_revenue", id, null, null, null);
        return sale(id);
    }

    @PostMapping("/api/v1/sales-revenues/{id}/confirm-payment")
    @Transactional
    public Map<String, Object> confirmPayment(@PathVariable long id) {
        long me = Principals.employeeId();
        Map<String, Object> m = sale(id);
        if (!isTitle(me, "FIN_MANAGER")) {
            throw new AccessDeniedException("Chỉ Finance Manager xác nhận tiền thực nhận");
        }
        if (!"OWNER_CONFIRMED".equals(m.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Phải xác nhận Sales owner trước khi xác nhận tiền thực nhận");
        }
        if (m.get("paymentDate") == null) {
            throw BusinessException.unprocessable("PAYMENT_DATE_REQUIRED", "Phải có ngày thực nhận tiền");
        }
        jdbc.update("UPDATE sales_revenue SET status = 'CONFIRMED', finance_confirmed_by = ?, finance_confirmed_at = now() WHERE id = ?", me, id);
        audit.success("CONFIRM_SALES_PAYMENT", "sales_revenue", id, null, null, null);
        return sale(id);
    }

    @PostMapping("/api/v1/sales-revenues/{id}/reject")
    @Transactional
    public Map<String, Object> rejectSale(@PathVariable long id, @RequestBody(required = false) NoteBody b) {
        long me = Principals.employeeId();
        Map<String, Object> m = sale(id);
        long owner = ((Number) m.get("ownerEmployeeId")).longValue();
        boolean allowed = (isTitle(me, "SALES_MANAGER") && employees.isTeamMember(me, owner)) || isTitle(me, "FIN_MANAGER");
        if (!allowed) {
            throw new AccessDeniedException("Không có quyền từ chối khoản doanh thu");
        }
        if ("CONFIRMED".equals(m.get("status")) || "REJECTED".equals(m.get("status"))) {
            throw BusinessException.conflict("INVALID_STATE", "Khoản đã chốt hoặc đã bị từ chối");
        }
        jdbc.update("UPDATE sales_revenue SET status = 'REJECTED', note = COALESCE(?, note) WHERE id = ?", b == null ? null : b.note(), id);
        audit.success("REJECT_SALES_REVENUE", "sales_revenue", id, b == null ? null : b.note(), null, null);
        return sale(id);
    }
}
