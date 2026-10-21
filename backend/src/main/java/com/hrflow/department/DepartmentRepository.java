package com.hrflow.department;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DepartmentRepository extends JpaRepository<Department, Long> {

    boolean existsByDepartmentCode(String departmentCode);

    boolean existsByDepartmentNameIgnoreCase(String departmentName);

    boolean existsByDepartmentNameIgnoreCaseAndIdNot(String departmentName, Long id);

    List<Department> findAllByOrderByDepartmentNameAsc();

    List<Department> findAllByActiveTrueOrderByDepartmentNameAsc();
}