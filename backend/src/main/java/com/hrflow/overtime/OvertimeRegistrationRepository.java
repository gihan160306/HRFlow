
package com.hrflow.overtime;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OvertimeRegistrationRepository
        extends JpaRepository<OvertimeRegistration, Long> {

    Optional<OvertimeRegistration>
    findByScheduleIdAndEmployeeId(
            Long scheduleId,
            Long employeeId
    );

    List<OvertimeRegistration>
    findByEmployeeIdOrderByCreatedAtDesc(Long employeeId);

    List<OvertimeRegistration>
    findByStatusOrderByCreatedAtAsc(OvertimeStatus status);

    List<OvertimeRegistration>
    findByScheduleIdAndStatus(
            Long scheduleId,
            OvertimeStatus status
    );

    List<OvertimeRegistration>
    findByEmployeeIdAndStatus(
            Long employeeId,
            OvertimeStatus status
    );
}
