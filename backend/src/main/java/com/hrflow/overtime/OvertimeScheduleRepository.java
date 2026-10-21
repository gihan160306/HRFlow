
package com.hrflow.overtime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OvertimeScheduleRepository
        extends JpaRepository<OvertimeSchedule, Long> {

    List<OvertimeSchedule>
    findByStatusOrderByStartAtAsc(
            OvertimeSchedule.ScheduleStatus status
    );

    Optional<OvertimeSchedule> findByScheduleIdAndStatus(
            Long scheduleId,
            OvertimeSchedule.ScheduleStatus status
    );

    @Query("""
        SELECT s
        FROM OvertimeSchedule s
        WHERE s.status = :status
          AND s.startAt < :endAt
          AND s.endAt > :startAt
        ORDER BY s.startAt ASC
        """)
    List<OvertimeSchedule> findOverlappingSchedules(
            @Param("status")
            OvertimeSchedule.ScheduleStatus status,
            @Param("startAt") LocalDateTime startAt,
            @Param("endAt") LocalDateTime endAt
    );
}
