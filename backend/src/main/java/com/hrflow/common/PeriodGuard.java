package com.hrflow.common;

import java.time.LocalDate;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.hrflow.common.exception.BusinessException;

/**
 * Chặn sửa dữ liệu nguồn (công, phép, OT, thu nhập, hồ sơ có hiệu lực) của tháng đã chốt công
 * hoặc kỳ lương đã APPROVED/LOCKED. Sai sót sau khi khóa phải đi qua payroll_adjustment ở kỳ sau.
 */
@Component
public class PeriodGuard {

    private final JdbcTemplate jdbc;

    public PeriodGuard(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isFrozen(LocalDate from, LocalDate to) {
        Integer n = jdbc.queryForObject("""
                SELECT COUNT(*) FROM payroll_period
                WHERE start_date <= ? AND end_date >= ? AND (timesheet_closed OR status IN ('APPROVED', 'LOCKED'))
                """, Integer.class, to, from);
        return n != null && n > 0;
    }

    public void assertOpen(LocalDate date) {
        assertOpen(date, date);
    }

    public void assertOpen(LocalDate from, LocalDate to) {
        if (isFrozen(from, to)) {
            throw BusinessException.conflict("PERIOD_FROZEN",
                    "Tháng " + from + " đã chốt công/khóa lương, không được sửa dữ liệu nguồn");
        }
    }
}
