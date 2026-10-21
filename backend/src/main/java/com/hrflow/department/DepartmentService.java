package com.hrflow.department;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.exception.ResourceNotFoundException;
import com.hrflow.employee.Employee;
import com.hrflow.employee.EmployeeRepository;

@Service
@Transactional(readOnly = true)
public class DepartmentService {

    private final DepartmentRepository departmentRepository;
    private final EmployeeRepository employeeRepository;

    public DepartmentService(DepartmentRepository departmentRepository, EmployeeRepository employeeRepository) {
        this.departmentRepository = departmentRepository;
        this.employeeRepository = employeeRepository;
    }

    public List<DepartmentResponse> list(boolean activeOnly) {
        List<Department> list = activeOnly
                ? departmentRepository.findAllByActiveTrueOrderByDepartmentNameAsc()
                : departmentRepository.findAllByOrderByDepartmentNameAsc();
        return list.stream().map(DepartmentResponse::from).toList();
    }

    public DepartmentResponse get(Long departmentId) {
        return DepartmentResponse.from(find(departmentId));
    }

    @Transactional
    public DepartmentResponse create(DepartmentCreateRequest r) {
        String code = r.departmentCode().trim().toUpperCase(Locale.ROOT);
        String name = r.departmentName().trim();

        if (departmentRepository.existsByDepartmentCode(code)) {
            throw BusinessException.conflict("DEPARTMENT_CODE_EXISTS", "Mã phòng ban đã tồn tại: " + code);
        }
        if (departmentRepository.existsByDepartmentNameIgnoreCase(name)) {
            throw BusinessException.conflict("DEPARTMENT_NAME_EXISTS", "Tên phòng ban đã tồn tại: " + name);
        }

        Department d = new Department();
        d.setDepartmentCode(code);
        d.setDepartmentName(name);
        d.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        d.setActive(true);
        return DepartmentResponse.from(departmentRepository.save(d));
    }

    @Transactional
    public DepartmentResponse update(Long departmentId, DepartmentUpdateRequest r) {
        Department d = find(departmentId);

        if (r.departmentName() != null) {
            String name = r.departmentName().trim();
            if (!name.equalsIgnoreCase(d.getDepartmentName())
                    && departmentRepository.existsByDepartmentNameIgnoreCaseAndIdNot(name, departmentId)) {
                throw BusinessException.conflict("DEPARTMENT_NAME_EXISTS", "Tên phòng ban đã tồn tại: " + name);
            }
            d.setDepartmentName(name);
        }
        if (r.description() != null) {
            d.setDescription(r.description().isBlank() ? null : r.description().trim());
        }

        if (r.active() != null && r.active() != d.isActive()) {
            // Không xóa phòng ban; chỉ ngừng hoạt động khi không còn nhân viên đang làm việc.
            if (!r.active() && employeeRepository.existsByDepartmentIdAndStatus(departmentId, Employee.Status.ACTIVE)) {
                throw BusinessException.conflict("DEPARTMENT_HAS_ACTIVE_EMPLOYEES",
                        "Không thể ngừng hoạt động: phòng ban còn nhân viên đang làm việc. "
                                + "Hãy chuyển nhân viên sang phòng ban khác trước.");
            }
            d.setActive(r.active());
        }
        return DepartmentResponse.from(departmentRepository.save(d));
    }

    private Department find(Long departmentId) {
        return departmentRepository.findById(departmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Phòng ban", departmentId));
    }
}