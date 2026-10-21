package com.hrflow.contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.employee.Employee;
import com.hrflow.employee.EmployeeRepository;
import com.hrflow.security.AuthUser;

@Service
@Transactional(readOnly = true)
public class ContractService {

    private static final Logger log = LoggerFactory.getLogger(ContractService.class);
    private static final ZoneId VN = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<String> HR_ADMIN = Set.of("HR", "ADMIN");
    private static final LocalDate FAR_FUTURE = LocalDate.of(9999, 12, 31);
    private static final BigDecimal DEFAULT_PROBATION_RATE = new BigDecimal("0.850");

    private final ContractRepository contractRepository;
    private final EmployeeRepository employeeRepository;
    private final BigDecimal regionalMinWage;

    public ContractService(ContractRepository contractRepository,
                           EmployeeRepository employeeRepository,
                           // TODO: thay bằng tra cứu system_parameter 'regional_min_wage' theo ngày hiệu lực.
                           @Value("${hrflow.payroll.regional-min-wage}") BigDecimal regionalMinWage) {
        this.contractRepository = contractRepository;
        this.employeeRepository = employeeRepository;
        this.regionalMinWage = regionalMinWage;
    }

    // ===================== Truy vấn =====================

    /** HR/Admin: tất cả. Manager: của mình + team trực tiếp. Employee: của mình. */
    public List<ContractResponse> list(AuthUser user, Long employeeId, ContractStatus status) {
        Specification<Contract> spec = (root, q, cb) -> cb.conjunction();
        Long me = user.employeeId();

        if (!isHrOrAdmin(user)) {
            if (me == null) throw new AccessDeniedException("Tài khoản chưa liên kết hồ sơ nhân viên");
            if ("MANAGER".equals(user.role())) {
                spec = spec.and((root, q, cb) -> cb.or(
                        cb.equal(root.get("employee").get("id"), me),
                        cb.equal(root.get("employee").get("managerId"), me)));
            } else {
                if (employeeId != null && !employeeId.equals(me)) {
                    throw new AccessDeniedException("Không có quyền xem hợp đồng của nhân viên khác");
                }
                spec = spec.and((root, q, cb) -> cb.equal(root.get("employee").get("id"), me));
            }
        }
        if (employeeId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("employee").get("id"), employeeId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        List<Contract> contracts = contractRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "startDate"));
        return contracts.stream()
                .map(c -> ContractResponse.from(c, canSeeSalary(user, c)))
                .toList();
    }

    public ContractResponse getById(AuthUser user, Long contractId) {
        Contract c = find(contractId);
        assertCanView(user, c);
        return ContractResponse.from(c, canSeeSalary(user, c));
    }

    // ===================== Ghi =====================

    @Transactional
    public ContractResponse create(AuthUser user, ContractRequest r) {
        Employee employee = employeeRepository.findById(r.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", r.employeeId()));
        if (employee.isTerminated()) {
            throw BusinessException.conflict("EMPLOYEE_TERMINATED", "Nhân viên đã nghỉ việc, không thể tạo hợp đồng");
        }
        String contractNo = r.contractNo().trim();
        if (contractRepository.existsByContractNo(contractNo)) {
            throw BusinessException.conflict("CONTRACT_NO_EXISTS", "Số hợp đồng đã tồn tại: " + contractNo);
        }

        Contract c = new Contract();
        c.setEmployee(employee);
        c.setContractNo(contractNo);
        applyTerms(c, r.contractType(), r.startDate(), r.endDate(),
                money(r.baseSalary()), money(r.monthlyStipend()), r.probationRate(),
                money(r.insuranceSalary()), r.probationInsured());
        assertNoOverlap(employee.getId(), c.getStartDate(), c.getEndDate());

        c.setStatus(ContractStatus.derive(c.getEndDate(), today()));
        c.setNote(trimToNull(r.note()));
        c.setCreatedBy(user.userId());
        // TODO: ghi audit_log (CREATE contract) — mức lương là dữ liệu nhạy cảm.
        return ContractResponse.from(contractRepository.save(c), true);
    }

    /** SR-02.3: tạo giai đoạn mới nối tiếp, không tạo hồ sơ nhân viên mới. Hợp đồng cũ giữ nguyên. */
    @Transactional
    public ContractResponse renew(AuthUser user, Long contractId, ContractRenewRequest r) {
        Contract old = find(contractId);

        if (old.getStatus() == ContractStatus.TERMINATED) {
            throw BusinessException.conflict("CONTRACT_TERMINATED", "Hợp đồng đã kết thúc, không thể gia hạn");
        }
        if (old.getContractType() != Contract.ContractType.FIXED_TERM
                && old.getContractType() != Contract.ContractType.INTERNSHIP) {
            throw BusinessException.unprocessable("CONTRACT_NOT_RENEWABLE",
                    "Chỉ hợp đồng xác định thời hạn hoặc thực tập mới gia hạn được. "
                            + "Với thử việc hoặc chuyển loại hợp đồng, hãy tạo hợp đồng mới.");
        }
        if (contractRepository.existsByRenewedFromContractId(contractId)) {
            throw BusinessException.conflict("ALREADY_RENEWED", "Hợp đồng này đã được gia hạn");
        }
        if (!r.newStartDate().isAfter(old.getEndDate())) {
            throw BusinessException.unprocessable("RENEW_START_INVALID",
                    "Ngày bắt đầu mới phải sau ngày kết thúc của hợp đồng hiện tại (" + old.getEndDate() + ")");
        }
        String contractNo = r.contractNo().trim();
        if (contractRepository.existsByContractNo(contractNo)) {
            throw BusinessException.conflict("CONTRACT_NO_EXISTS", "Số hợp đồng đã tồn tại: " + contractNo);
        }

        Contract c = new Contract();
        c.setEmployee(old.getEmployee());
        c.setContractNo(contractNo);
        c.setRenewedFromContractId(old.getId());
        applyTerms(c, old.getContractType(), r.newStartDate(), r.newEndDate(),
                r.baseSalary() != null ? money(r.baseSalary()) : old.getBaseSalary(),
                r.monthlyStipend() != null ? money(r.monthlyStipend()) : old.getMonthlyStipend(),
                old.getProbationRate(),
                r.insuranceSalary() != null ? money(r.insuranceSalary()) : old.getInsuranceSalary(),
                old.isProbationInsured());
        assertNoOverlap(c.getEmployee().getId(), c.getStartDate(), c.getEndDate());

        c.setStatus(ContractStatus.derive(c.getEndDate(), today()));
        c.setNote(trimToNull(r.note()));
        c.setCreatedBy(user.userId());
        // TODO: ghi audit_log (RENEW contract).
        return ContractResponse.from(contractRepository.save(c), true);
    }

    /**
     * Kết thúc hợp đồng. Hợp đồng có thời hạn: end_date được rút về ngày kết thúc.
     * INDEFINITE: ràng buộc CK (INDEFINITE ⇔ end_date IS NULL) nên chỉ đổi trạng thái, giữ end_date NULL.
     */
    @Transactional
    public ContractResponse terminate(Long contractId, ContractTerminateRequest r) {
        Contract c = find(contractId);
        if (c.getStatus() == ContractStatus.TERMINATED) {
            throw BusinessException.conflict("ALREADY_TERMINATED", "Hợp đồng đã được kết thúc trước đó");
        }
        LocalDate d = r.terminationDate();
        if (d.isBefore(c.getStartDate())) {
            throw BusinessException.unprocessable("TERMINATION_BEFORE_START", "Ngày kết thúc không được trước ngày bắt đầu");
        }
        if (c.getEndDate() != null) {
            if (d.isAfter(c.getEndDate())) {
                throw BusinessException.unprocessable("TERMINATION_AFTER_END",
                        "Ngày kết thúc không được sau ngày hết hạn hợp đồng (" + c.getEndDate() + ")");
            }
            c.setEndDate(d);
        }
        c.setStatus(ContractStatus.TERMINATED);
        String note = (c.getNote() == null ? "" : c.getNote() + " | ") + "Kết thúc: " + r.reason().trim();
        c.setNote(note.length() > 500 ? note.substring(0, 500) : note);
        // TODO: ghi audit_log (TERMINATE contract).
        return ContractResponse.from(contractRepository.save(c), true);
    }

    // ===================== Job cập nhật trạng thái (SR-02.1, SR-02.2) =====================

    /**
     * Chạy hằng ngày lúc 01:00 (giờ Việt Nam). Cần @EnableScheduling ở một lớp @Configuration.
     * Nhiều instance chạy song song thì thêm khóa phân tán (ví dụ ShedLock) để tránh gửi thông báo trùng.
     */
    @Scheduled(cron = "${hrflow.contract.status-cron:0 0 1 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void refreshStatuses() {
        LocalDate today = today();

        List<Contract> expired = contractRepository.findByStatusInAndEndDateLessThan(
                List.of(ContractStatus.ACTIVE, ContractStatus.EXPIRING_SOON), today);
        expired.forEach(c -> c.setStatus(ContractStatus.EXPIRED));

        // Hợp đồng đã được gia hạn thì không cảnh báo nữa.
        List<Contract> expiringSoon = contractRepository
                .findByStatusAndEndDateBetween(ContractStatus.ACTIVE, today,
                        today.plusDays(ContractStatus.EXPIRING_SOON_DAYS))
                .stream()
                .filter(c -> !contractRepository.existsByRenewedFromContractId(c.getId()))
                .toList();
        expiringSoon.forEach(c -> c.setStatus(ContractStatus.EXPIRING_SOON));

        log.info("Cập nhật trạng thái hợp đồng: {} hết hạn, {} sắp hết hạn", expired.size(), expiringSoon.size());
        // TODO (SR-02.2): gửi notification cho HR với danh sách `expiringSoon`.
    }

    // ===================== Quy tắc / tiện ích =====================

    /** Kiểm tra và gán điều khoản theo loại hợp đồng (data_dictionary: các CK cấp bảng + insurance_salary). */
    private void applyTerms(Contract c, Contract.ContractType type, LocalDate start, LocalDate end,
                            BigDecimal baseSalary, BigDecimal stipend, BigDecimal probationRate,
                            BigDecimal insuranceSalary, Boolean probationInsured) {
        if (type == Contract.ContractType.INDEFINITE) {
            if (end != null) {
                throw BusinessException.unprocessable("INDEFINITE_NO_END_DATE",
                        "Hợp đồng không xác định thời hạn không có ngày kết thúc");
            }
        } else {
            if (end == null) {
                throw BusinessException.unprocessable("END_DATE_REQUIRED", "Hợp đồng có thời hạn phải có ngày kết thúc");
            }
            if (end.isBefore(start)) {
                throw BusinessException.unprocessable("INVALID_DATE_RANGE", "Ngày kết thúc phải từ ngày bắt đầu trở đi");
            }
        }

        if (type == Contract.ContractType.INTERNSHIP) {
            if (stipend == null) {
                throw BusinessException.unprocessable("STIPEND_REQUIRED", "Hợp đồng thực tập bắt buộc có trợ cấp hàng tháng");
            }
            c.setMonthlyStipend(stipend);
            c.setBaseSalary(null);
        } else {
            if (baseSalary == null) {
                throw BusinessException.unprocessable("BASE_SALARY_REQUIRED", "Bắt buộc có lương cơ bản");
            }
            c.setBaseSalary(baseSalary);
            c.setMonthlyStipend(null);
        }

        if (type == Contract.ContractType.PROBATION) {
            c.setProbationRate(probationRate != null ? probationRate : DEFAULT_PROBATION_RATE);
            c.setProbationInsured(Boolean.TRUE.equals(probationInsured));
        } else {
            c.setProbationRate(null);
            c.setProbationInsured(false);
        }

        if (insuranceSalary != null && insuranceSalary.compareTo(regionalMinWage) < 0) {
            throw BusinessException.unprocessable("INSURANCE_SALARY_BELOW_MIN",
                    "Mức lương đóng bảo hiểm phải từ lương tối thiểu vùng trở lên (" + regionalMinWage.toPlainString() + ")");
        }
        c.setInsuranceSalary(insuranceSalary);

        c.setContractType(type);
        c.setStartDate(start);
        c.setEndDate(end);
    }

    private void assertNoOverlap(Long employeeId, LocalDate start, LocalDate end) {
        LocalDate effectiveEnd = end != null ? end : FAR_FUTURE;
        if (contractRepository.existsOverlapping(employeeId, start, effectiveEnd)) {
            throw BusinessException.conflict("CONTRACT_OVERLAP",
                    "Khoảng thời gian trùng với một hợp đồng khác của nhân viên này");
        }
    }

    private Contract find(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() -> new ResourceNotFoundException("Hợp đồng", contractId));
    }

    private void assertCanView(AuthUser user, Contract c) {
        if (isHrOrAdmin(user)) return;
        Long me = user.employeeId();
        if (me != null) {
            if (me.equals(c.getEmployee().getId())) return;
            if ("MANAGER".equals(user.role()) && me.equals(c.getEmployee().getManagerId())) return;
        }
        throw new AccessDeniedException("Không có quyền xem hợp đồng này");
    }

    private boolean canSeeSalary(AuthUser user, Contract c) {
        return isHrOrAdmin(user) || (user.employeeId() != null && user.employeeId().equals(c.getEmployee().getId()));
    }

    private boolean isHrOrAdmin(AuthUser user) {
        return HR_ADMIN.contains(user.role());
    }

    private static LocalDate today() {
        return LocalDate.now(VN);
    }

    private static BigDecimal money(Long v) {
        return v == null ? null : BigDecimal.valueOf(v);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}