package com.hrflow.payroll;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

@Repository
public interface PayrollPeriodRepository extends JpaRepository<PayrollPeriod, Long> {

    Optional<PayrollPeriod> findByPeriodCode(String periodCode);

    List<PayrollPeriod> findAllByOrderByStartDateDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PayrollPeriod p where p.id = :id")
    Optional<PayrollPeriod> findLockedById(Long id);
}
