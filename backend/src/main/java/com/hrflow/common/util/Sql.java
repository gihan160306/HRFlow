package com.hrflow.common.util;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Chuyển kết quả JdbcTemplate (snake_case) sang JSON camelCase theo hợp đồng OpenAPI. */
public final class Sql {

    private Sql() {
    }

    public static Map<String, Object> camel(Map<String, Object> row) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : row.entrySet()) {
            out.put(camelKey(e.getKey()), convert(e.getValue()));
        }
        return out;
    }

    public static List<Map<String, Object>> camel(List<Map<String, Object>> rows) {
        List<Map<String, Object>> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            out.add(camel(r));
        }
        return out;
    }

    /** Một dòng (camelCase) hoặc 404 nếu không có. */
    public static Map<String, Object> one(org.springframework.jdbc.core.JdbcTemplate jdbc, String notFound, String sql,
                                          Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        if (rows.isEmpty()) {
            throw new com.hrflow.common.exception.ResourceNotFoundException(notFound);
        }
        return camel(rows.get(0));
    }

    public static List<Map<String, Object>> list(org.springframework.jdbc.core.JdbcTemplate jdbc, String sql,
                                                 Object... args) {
        return camel(jdbc.queryForList(sql, args));
    }

    public static String camelKey(String key) {
        if (key.indexOf('_') < 0) {
            return key;
        }
        StringBuilder sb = new StringBuilder();
        boolean up = false;
        for (char c : key.toCharArray()) {
            if (c == '_') {
                up = true;
            } else {
                sb.append(up ? Character.toUpperCase(c) : c);
                up = false;
            }
        }
        return sb.toString();
    }

    private static Object convert(Object v) {
        if (v instanceof Timestamp t) {
            return t.toInstant();
        }
        if (v instanceof OffsetDateTime o) {
            return o.toInstant();
        }
        if (v instanceof Date d) {
            return d.toLocalDate();
        }
        if (v instanceof Time t) {
            return t.toLocalTime();
        }
        if (v instanceof BigDecimal b && b.scale() > 0) {
            BigDecimal s = b.stripTrailingZeros();
            return s.scale() < 0 ? s.setScale(0) : s;
        }
        if (v instanceof org.postgresql.util.PGobject p) {
            return p.getValue();
        }
        return v;
    }
}
