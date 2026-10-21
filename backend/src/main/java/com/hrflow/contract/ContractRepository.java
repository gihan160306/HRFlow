package com.hrflow.contract;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ContractRepository extends JpaRepository<Contract, Long>, JpaSpecificationExecutor<Contract> {

    boolean existsByContractNo(String contractNo);

    boolean existsByRenewedFromContractId(Long contractId);

    /**
     * Trùng khoảng thời gian với hợp đồng khác của cùng nhân viên (mọi trạng thái, giống ràng buộc EXCLUDE ở CSDL).
     * end = LocalDate.of(9999,12,31) khi hợp đồng không xác định thời hạn.
     */
    @Query("""
            select count(c) > 0 from Contract c
            where c.employee.id = :employeeId
              and c.startDate <= :end
              and (c.endDate is null or c.endDate >= :start)
            """)
    boolean existsOverlapping(@Param("employeeId") Long employeeId,
                              @Param("start") LocalDate start,
                              @Param("end") LocalDate end);

    List<Contract> findByStatusInAndEndDateLessThan(Collection<ContractStatus> statuses, LocalDate date);

    List<Contract> findByStatusAndEndDateBetween(ContractStatus status, LocalDate from, LocalDate to);

    @Override
    @EntityGraph(attributePaths = "employee")
    List<Contract> findAll(Specification<Contract> spec, Sort sort);
}