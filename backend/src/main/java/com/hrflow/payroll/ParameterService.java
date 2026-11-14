package com.hrflow.payroll;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.hrflow.common.exception.BusinessException;

/**
 * Tham số payroll có ngày hiệu lực, nguồn và trạng thái xác nhận (payroll_rules Mục 2.2, 12.1).
 * Không còn giá trị viết cứng (trần bảo hiểm, giảm trừ, lương tối thiểu vùng...): thiếu bộ tham số phù hợp thì
 * báo lỗi 422, không tự lấy bộ mới nhất.
 */
@Service
public class ParameterService {

    public record Param(String code, BigDecimal value, String status, String sourceRef, LocalDate effectiveFrom,
                        int version, String region) {
        public boolean needsReview() {
            return !"VERIFIED_SIM".equals(status);
        }
    }

    private final JdbcTemplate jdbc;

    public ParameterService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Tham số hiệu lực tại {@code date}; region = null với tham số không theo vùng. */
    public Param get(String code, LocalDate date, String region) {
        List<Param> rows = jdbc.query("""
                SELECT code, value, status, source_ref, effective_from, param_version, region FROM payroll_parameter
                WHERE code = ? AND COALESCE(region, '-') = COALESCE(?, '-')
                  AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?)
                ORDER BY effective_from DESC, param_version DESC LIMIT 1
                """, (rs, i) -> new Param(rs.getString(1), rs.getBigDecimal(2), rs.getString(3), rs.getString(4),
                rs.getDate(5).toLocalDate(), rs.getInt(6), rs.getString(7)), code, region, date, date);
        if (rows.isEmpty()) {
            throw BusinessException.unprocessable("PARAMETER_MISSING", "Thiếu tham số " + code
                    + (region == null ? "" : " (" + region + ")") + " có hiệu lực tại " + date);
        }
        return rows.get(0);
    }

    public BigDecimal value(String code, LocalDate date) {
        return get(code, date, null).value();
    }

    public Map<String, Object> asMap(Param p) {
        return Map.of("code", p.code(), "value", p.value(), "status", p.status(), "sourceRef", p.sourceRef(),
                "effectiveFrom", p.effectiveFrom().toString(), "version", p.version());
    }
}
