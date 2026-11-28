package com.hrflow.overtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.hrflow.common.exception.BusinessException;
import com.hrflow.common.util.Vn;
import com.hrflow.overtime.OvertimeService.UnscheduledSegment;

class OvertimeServiceTest {

    private static Instant at(int year, int month, int day, int hour, int minute) {
        return ZonedDateTime.of(LocalDate.of(year, month, day), LocalTime.of(hour, minute), Vn.ZONE).toInstant();
    }

    @Test
    void overnightOtSplitsAtLocalMidnightWithoutDuplicateMinutes() {
        Instant start = at(2026, 11, 25, 23, 30);
        Instant end = at(2026, 11, 26, 0, 30);

        List<UnscheduledSegment> parts = OvertimeService.splitUnscheduledInterval(start, end);

        assertEquals(2, parts.size());
        assertEquals(new UnscheduledSegment(LocalDate.of(2026, 11, 25), start, at(2026, 11, 26, 0, 0)), parts.get(0));
        assertEquals(new UnscheduledSegment(LocalDate.of(2026, 11, 26), at(2026, 11, 26, 0, 0), end), parts.get(1));
        assertEquals(parts.get(0).end(), parts.get(1).start());
    }

    @Test
    void monthEndOtBelongsToBothPayrollMonths() {
        List<UnscheduledSegment> parts = OvertimeService.splitUnscheduledInterval(
                at(2026, 11, 30, 23, 30), at(2026, 12, 1, 0, 30));

        assertEquals(List.of(LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 1)),
                parts.stream().map(UnscheduledSegment::date).toList());
    }

    @Test
    void endExactlyAtMidnightDoesNotCreateEmptyNextDay() {
        List<UnscheduledSegment> parts = OvertimeService.splitUnscheduledInterval(
                at(2026, 11, 25, 23, 30), at(2026, 11, 26, 0, 0));

        assertEquals(1, parts.size());
        assertEquals(LocalDate.of(2026, 11, 25), parts.get(0).date());
    }

    @Test
    void rejectsSubMinuteIntervalsThatCannotBePaidExactly() {
        Instant start = at(2026, 11, 25, 23, 30).plusSeconds(30);
        assertThrows(BusinessException.class, () -> OvertimeService.splitUnscheduledInterval(start, start.plusSeconds(60)));
    }
}
