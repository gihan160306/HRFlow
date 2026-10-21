package com.hrflow.leave;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface LeaveRepository extends JpaRepository<LeaveRequest,Long> {
 List<LeaveRequest> findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);
 Optional<LeaveRequest> findByIdAndEmployeeId(Long id,Long employeeId);
 @Query("select count(l)>0 from LeaveRequest l where l.employeeId=:eid and l.status in :statuses and l.startDate<=:endDate and l.endDate>=:startDate and (:excludeId is null or l.id<>:excludeId)")
 boolean hasOverlap(@Param("eid") Long employeeId,@Param("startDate") LocalDate start,@Param("endDate") LocalDate end,@Param("statuses") List<LeaveStatus> statuses,@Param("excludeId") Long excludeId);
}
