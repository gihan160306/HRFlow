package com.hrflow.employee;

import java.util.Locale;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.common.response.PageResponse;
import com.hrflow.department.Department;
import com.hrflow.department.DepartmentRepository;
import com.hrflow.security.AuthUser;

@Service
@Transactional(readOnly = true)
public class EmployeeService {

    private static final Set<String> HR_ADMIN = Set.of("HR", "ADMIN");
    private static final int MAX_MANAGER_CHAIN = 50;

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    public EmployeeService(EmployeeRepository employeeRepository, DepartmentRepository departmentRepository) {
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    // ===================== Truy vấn =====================

    /** HR/Admin xem toàn bộ; Manager chỉ thấy nhân viên có manager_id = mình (team trực tiếp). */
    public PageResponse<EmployeeResponse> search(AuthUser user, Long departmentId, Employee.Status status,
                                                 Pageable pageable) {
        Specification<Employee> spec = (root, query, cb) -> cb.conjunction();

        if (departmentId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("department").get("id"), departmentId));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }

        boolean hrAdmin = isHrOrAdmin(user);
        if (!hrAdmin) {
            if (!"MANAGER".equals(user.role()) || user.employeeId() == null) {
                throw new AccessDeniedException("Không có quyền xem danh sách nhân viên");
            }
            Long managerEmployeeId = user.employeeId();
            spec = spec.and((root, q, cb) -> cb.equal(root.get("managerId"), managerEmployeeId));
        }

        Page<Employee> page = employeeRepository.findAll(spec, pageable);
        return PageResponse.from(page.map(e -> EmployeeResponse.from(e, hrAdmin)));
    }

    public EmployeeResponse getById(AuthUser user, Long employeeId) {
        Employee e = find(employeeId);
        assertCanView(user, e);
        return EmployeeResponse.from(e, isHrOrAdmin(user));
    }

    // ===================== Tạo / cập nhật =====================

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest r) {
        String code = r.employeeCode().trim().toUpperCase(Locale.ROOT);
        String email = r.workEmail().trim().toLowerCase(Locale.ROOT);
        String nationalId = trimToNull(r.nationalId());

        if (employeeRepository.existsByEmployeeCode(code)) {
            throw BusinessException.conflict("EMPLOYEE_CODE_EXISTS", "Mã nhân viên đã tồn tại: " + code);
        }
        if (employeeRepository.existsByWorkEmail(email)) {
            throw BusinessException.conflict("EMAIL_EXISTS", "Email công việc đã được sử dụng");
        }
        if (nationalId != null && employeeRepository.existsByNationalId(nationalId)) {
            throw BusinessException.conflict("NATIONAL_ID_EXISTS", "CCCD đã tồn tại trong hệ thống");
        }

        Employee e = new Employee();
        e.setEmployeeCode(code);
        e.setFullName(r.fullName().trim());
        e.setWorkEmail(email);
        e.setPhone(trimToNull(r.phone()));
        e.setDateOfBirth(r.dateOfBirth());
        e.setAddress(trimToNull(r.address()));
        e.setNationalId(nationalId);
        e.setTaxCode(trimToNull(r.taxCode()));
        e.setBankName(trimToNull(r.bankName()));
        e.setBankAccountNo(trimToNull(r.bankAccountNo()));
        e.setDepartment(requireActiveDepartment(r.departmentId()));
        e.setJobTitle(r.jobTitle().trim());
        e.setEmployeeType(r.employeeType());
        e.setManagerId(r.managerId() == null ? null : requireValidManager(r.managerId(), null).getId());
        e.setHireDate(r.hireDate());
        e.setStatus(Employee.Status.ACTIVE);
        e.setDependentsCount(r.dependentsCount() == null ? 0 : r.dependentsCount().shortValue());
        e.setTaxCommitment08(bool(r.taxCommitment08()));
        e.setOtEligible(bool(r.otEligible()));
        e.setCommissionEligible(bool(r.commissionEligible()));
        e.setKpiEligible(bool(r.kpiEligible()));
        e.setPerformanceBonusEligible(bool(r.performanceBonusEligible()));
        e.setTeamBonusEligible(bool(r.teamBonusEligible()));
        e.setLunchAllowanceEligible(bool(r.lunchAllowanceEligible()));
        e.setParkingAllowanceEligible(bool(r.parkingAllowanceEligible()));
        e.setPhoneAllowanceEligible(bool(r.phoneAllowanceEligible()));
        e.setPositionAllowanceEligible(bool(r.positionAllowanceEligible()));

        validatePayrollRules(e);
        // TODO: ghi audit_log (CREATE employee) — che CCCD/MST/ngân hàng khi ghi old/new value.
        return EmployeeResponse.from(employeeRepository.save(e), true);
    }

    @Transactional
    public EmployeeResponse update(Long employeeId, EmployeeUpdateRequest r) {
        Employee e = find(employeeId);
        if (e.isTerminated()) {
            throw BusinessException.conflict("EMPLOYEE_TERMINATED", "Hồ sơ nhân viên đã nghỉ việc, không thể chỉnh sửa");
        }

        if (r.workEmail() != null) {
            String email = r.workEmail().trim().toLowerCase(Locale.ROOT);
            if (!email.equals(e.getWorkEmail())) {
                if (employeeRepository.existsByWorkEmail(email)) {
                    throw BusinessException.conflict("EMAIL_EXISTS", "Email công việc đã được sử dụng");
                }
                e.setWorkEmail(email);
            }
        }
        if (r.nationalId() != null) {
            String nid = r.nationalId().trim();
            if (!nid.equals(e.getNationalId())) {
                if (employeeRepository.existsByNationalId(nid)) {
                    throw BusinessException.conflict("NATIONAL_ID_EXISTS", "CCCD đã tồn tại trong hệ thống");
                }
                e.setNationalId(nid);
            }
        }

        // Chuyển phòng ban: phải tồn tại và đang hoạt động (chỉ kiểm tra khi thực sự đổi).
        if (r.departmentId() != null && !r.departmentId().equals(e.getDepartment().getId())) {
            e.setDepartment(requireActiveDepartment(r.departmentId()));
        }

        // Quản lý trực tiếp
        if (Boolean.TRUE.equals(r.removeManager())) {
            if (r.managerId() != null) {
                throw BusinessException.unprocessable("MANAGER_CONFLICT",
                        "Không thể vừa đặt managerId vừa removeManager");
            }
            e.setManagerId(null);
        } else if (r.managerId() != null && !r.managerId().equals(e.getManagerId())) {
            e.setManagerId(requireValidManager(r.managerId(), e.getId()).getId());
        }

        if (r.fullName() != null) e.setFullName(r.fullName().trim());
        if (r.phone() != null) e.setPhone(r.phone().trim());
        if (r.dateOfBirth() != null) e.setDateOfBirth(r.dateOfBirth());
        if (r.address() != null) e.setAddress(r.address().trim());
        if (r.taxCode() != null) e.setTaxCode(r.taxCode().trim());
        if (r.bankName() != null) e.setBankName(r.bankName().trim());
        if (r.bankAccountNo() != null) e.setBankAccountNo(r.bankAccountNo().trim());
        if (r.jobTitle() != null) e.setJobTitle(r.jobTitle().trim());
        if (r.employeeType() != null) e.setEmployeeType(r.employeeType());
        if (r.hireDate() != null) e.setHireDate(r.hireDate());
        if (r.dependentsCount() != null) e.setDependentsCount(r.dependentsCount().shortValue());
        if (r.taxCommitment08() != null) e.setTaxCommitment08(r.taxCommitment08());
        if (r.otEligible() != null) e.setOtEligible(r.otEligible());
        if (r.commissionEligible() != null) e.setCommissionEligible(r.commissionEligible());
        if (r.kpiEligible() != null) e.setKpiEligible(r.kpiEligible());
        if (r.performanceBonusEligible() != null) e.setPerformanceBonusEligible(r.performanceBonusEligible());
        if (r.teamBonusEligible() != null) e.setTeamBonusEligible(r.teamBonusEligible());
        if (r.lunchAllowanceEligible() != null) e.setLunchAllowanceEligible(r.lunchAllowanceEligible());
        if (r.parkingAllowanceEligible() != null) e.setParkingAllowanceEligible(r.parkingAllowanceEligible());
        if (r.phoneAllowanceEligible() != null) e.setPhoneAllowanceEligible(r.phoneAllowanceEligible());
        if (r.positionAllowanceEligible() != null) e.setPositionAllowanceEligible(r.positionAllowanceEligible());

        validatePayrollRules(e); // kiểm tra trạng thái cuối cùng sau khi áp dụng thay đổi
        // TODO: ghi audit_log (UPDATE employee) với old/new value đã che trường nhạy cảm.
        return EmployeeResponse.from(employeeRepository.save(e), true);
    }

    // ===================== Quy tắc / tiện ích =====================

    private Employee find(Long employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhân viên", employeeId));
    }

    /** Record-level: HR/Admin; chính chủ; Manager với nhân viên thuộc team trực tiếp. */
    private void assertCanView(AuthUser user, Employee target) {
        if (isHrOrAdmin(user)) return;
        Long me = user.employeeId();
        if (me != null) {
            if (me.equals(target.getId())) return;
            if ("MANAGER".equals(user.role()) && me.equals(target.getManagerId())) return;
        }
        throw new AccessDeniedException("Không có quyền xem hồ sơ nhân viên này");
    }

    private boolean isHrOrAdmin(AuthUser user) {
        return HR_ADMIN.contains(user.role());
    }

    private Department requireActiveDepartment(Long departmentId) {
        Department d = departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", departmentId));
        if (!d.isActive()) {
            throw BusinessException.unprocessable("DEPARTMENT_INACTIVE",
                    "Phòng ban đã ngừng hoạt động, không thể gán nhân viên: " + d.getDepartmentName());
        }
        return d;
    }

    /**
     * Manager phải tồn tại, đang ACTIVE, không phải chính nhân viên và không tạo vòng lặp
     * (A quản lý B, B lại quản lý A, trực tiếp hoặc gián tiếp).
     *
     * @param employeeId id nhân viên đang được sửa; null khi tạo mới
     */
    private Employee requireValidManager(Long managerId, Long employeeId) {
        Employee m = employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Quản lý trực tiếp", managerId));
        if (m.getStatus() != Employee.Status.ACTIVE) {
            throw BusinessException.unprocessable("MANAGER_NOT_ACTIVE", "Quản lý trực tiếp đã nghỉ việc");
        }
        if (employeeId != null) {
            if (employeeId.equals(managerId)) {
                throw BusinessException.unprocessable("MANAGER_SELF", "Nhân viên không thể là quản lý của chính mình");
            }
            Long current = m.getManagerId();
            int guard = 0;
            while (current != null && guard++ < MAX_MANAGER_CHAIN) {
                if (current.equals(employeeId)) {
                    throw BusinessException.unprocessable("MANAGER_CYCLE",
                            "Quan hệ quản lý bị vòng lặp (người này đang là cấp trên của quản lý được chọn)");
                }
                current = employeeRepository.findById(current).map(Employee::getManagerId).orElse(null);
            }
        }
        return m;
    }

    /** Ràng buộc nghiệp vụ mức bảng (data_dictionary.md): kiểm tra sớm để trả lỗi rõ ràng thay vì lỗi CHECK của CSDL. */
    private void validatePayrollRules(Employee e) {
        Employee.EmployeeType t = e.getEmployeeType();
        if ((t == Employee.EmployeeType.MANAGER || t == Employee.EmployeeType.INTERN) && e.isOtEligible()) {
            throw BusinessException.unprocessable("OT_NOT_ALLOWED",
                    "Nhân viên loại MANAGER hoặc INTERN không được đăng ký OT (ot_eligible phải là false)");
        }
        if (e.isCommissionEligible() && t != Employee.EmployeeType.SALES) {
            throw BusinessException.unprocessable("COMMISSION_ONLY_SALES",
                    "Chỉ nhân viên loại SALES được hưởng hoa hồng");
        }
    }

    private static boolean bool(Boolean b) {
        return Boolean.TRUE.equals(b);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}