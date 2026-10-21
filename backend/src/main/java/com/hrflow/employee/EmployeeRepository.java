package com.hrflow.employee;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByWorkEmail(String workEmail);

    boolean existsByNationalId(String nationalId);

    /** Dùng khi ngừng hoạt động phòng ban: còn nhân viên ACTIVE thì không được tắt. */
    boolean existsByDepartmentIdAndStatus(Long departmentId, Employee.Status status);

    /** Nạp sẵn department để tránh N+1 khi trả danh sách. */
    @Override
    @EntityGraph(attributePaths = "department")
    Page<Employee> findAll(Specification<Employee> spec, Pageable pageable);
}