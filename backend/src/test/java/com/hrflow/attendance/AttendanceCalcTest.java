package com.hrflow.attendance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.hrflow.attendance.AttendanceCalc.Result;
import com.hrflow.attendance.WorkCalendar.DayPlan;
import com.hrflow.attendance.WorkCalendar.Interval;
import com.hrflow.common.util.Vn;
import com.hrflow.leave.LeaveCalc;
import com.hrflow.leave.LeaveCalc.DayLeave;

class AttendanceCalcTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 14);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 13);

    private static final DayPlan FULL = new DayPlan(DAY, 1L, List.of(new Interval(LocalTime.of(8, 30), LocalTime.of(12, 0)),
            new Interval(LocalTime.of(13, 0), LocalTime.of(17, 30))), 480, WorkCalendar.WORKDAY, null);
    private static final DayPlan PART = new DayPlan(DAY, 2L, List.of(new Interval(LocalTime.of(8, 30), LocalTime.of(12, 30))), 240,
            WorkCalendar.WORKDAY, null);

    private static Instant at(int h, int m) {
        return ZonedDateTime.of(DAY, LocalTime.of(h, m), Vn.ZONE).toInstant();
    }

    @Test
    void fullDayHasNoLateOrEarly() {
        Result r = AttendanceCalc.compute(FULL, at(8, 30), at(17, 30), "MANUAL", TODAY);
        assertEquals(480, r.workedMinutes());
        assertEquals(0, r.lateMinutes());
        assertEquals("VALID", r.status());
    }

    @Test
    void tenMinutesLateIsRecordedFromFirstMinuteWithoutTolerance() {
        Result r = AttendanceCalc.compute(FULL, at(8, 40), at(17, 30), "MANUAL", TODAY);
        assertEquals(10, r.lateMinutes());
        assertEquals(470, r.workedMinutes());
        assertEquals("VALID", r.status());
    }

    @Test
    void lunchBreakIsNotWorkedOrLate() {
        Result r = AttendanceCalc.compute(FULL, at(8, 30), at(15, 0), "MANUAL", TODAY);
        assertEquals(210 + 120, r.workedMinutes());
        assertEquals(150, r.earlyMinutes());
    }

    @Test
    void partTimeUsesOwnSchedule() {
        Result r = AttendanceCalc.compute(PART, at(8, 30), at(12, 30), "MANUAL", TODAY);
        assertEquals(240, r.workedMinutes());
        assertEquals(240, r.scheduledMinutes());
    }

    @Test
    void missingCheckoutOnPastDayNeedsReviewNotZeroPay() {
        Result r = AttendanceCalc.compute(FULL, at(8, 30), null, "MANUAL", TODAY);
        assertEquals("NEEDS_REVIEW", r.status());
        assertEquals("Thiếu check-out", r.reviewReason());
    }

    @Test
    void openRecordToday_isStillValid() {
        DayPlan today = new DayPlan(TODAY, 1L, FULL.intervals(), 480, WorkCalendar.WORKDAY, null);
        Result r = AttendanceCalc.compute(today, ZonedDateTime.of(TODAY, LocalTime.of(8, 30), Vn.ZONE).toInstant(), null, "MANUAL", TODAY);
        assertEquals("VALID", r.status());
    }

    @Test
    void workOutsideScheduleNeedsReview() {
        DayPlan off = new DayPlan(DAY, 1L, List.of(), 0, WorkCalendar.WEEKLY_OFF, null);
        assertEquals("NEEDS_REVIEW", AttendanceCalc.compute(off, at(9, 0), at(11, 0), "MANUAL", TODAY).status());
    }

    @Test
    void halfDayLeaveUsesActualShiftDuration() {
        Map<LocalDate, DayPlan> plans = Map.of(DAY, FULL);
        DayLeave am = LeaveCalc.compute(plans, "HALF_DAY_AM", DAY, DAY, null, null).get(0);
        DayLeave pm = LeaveCalc.compute(plans, "HALF_DAY_PM", DAY, DAY, null, null).get(0);
        assertEquals(210, am.minutes());
        assertEquals(270, pm.minutes());
        assertEquals(120, LeaveCalc.compute(Map.of(DAY, PART), "HALF_DAY_AM", DAY, DAY, null, null).get(0).minutes());
        assertEquals(new java.math.BigDecimal("0.5000"), LeaveCalc.compute(Map.of(DAY, PART), "HALF_DAY_AM", DAY, DAY, null, null).get(0).dayFraction());
    }

    @Test
    void hourlyLeaveSkipsLunchBreak() {
        DayLeave d = LeaveCalc.compute(Map.of(DAY, FULL), "HOURS", DAY, DAY, LocalTime.of(11, 0), LocalTime.of(14, 0)).get(0);
        assertEquals(60 + 60, d.minutes());
    }
}
