package com.hrflow.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/** Múi giờ nghiệp vụ Asia/Ho_Chi_Minh và làm tròn VND (HALF_UP, payroll_rules.md Mục 10.1). */
public final class Vn {

    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private Vn() {
    }

    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    public static LocalDate dateOf(Instant instant) {
        return instant.atZone(ZONE).toLocalDate();
    }

    public static ZonedDateTime at(Instant instant) {
        return instant.atZone(ZONE);
    }

    /** ROUND_VND(x) = x.setScale(0, HALF_UP), kể cả số âm (-0,5 -> -1). */
    public static BigDecimal vnd(BigDecimal x) {
        return x.setScale(0, RoundingMode.HALF_UP);
    }
}
