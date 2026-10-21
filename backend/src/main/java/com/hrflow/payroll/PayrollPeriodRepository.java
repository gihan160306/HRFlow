package com.hrflow.payroll;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    Optional<PayrollPeriod> findByPeriodCode(String periodCode);

    List<PayrollPeriod> findAllByOrderByStartDateDesc();
}
