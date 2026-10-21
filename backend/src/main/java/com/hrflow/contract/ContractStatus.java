package com.hrflow.contract;

import java.time.LocalDate;

/**
 * Trạng thái hợp đồng (SR-02.1). Enum chỉ khai báo giá trị; việc chuyển trạng thái do
 * {@link ContractService#refreshStatuses()} (job chạy hằng ngày) và các nghiệp vụ renew/terminate thực hiện.
 */
public enum ContractStatus {
    ACTIVE,
    EXPIRING_SOON,
    EXPIRED,
    TERMINATED;

    /** Còn ≤ 30 ngày là sắp hết hạn (SR-02.1). */
    public static final int EXPIRING_SOON_DAYS = 30;

    /** Trạng thái theo thời hạn, chưa xét TERMINATED (TERMINATED chỉ đặt bằng nghiệp vụ kết thúc hợp đồng). */
    public static ContractStatus derive(LocalDate endDate, LocalDate today) {
        if (endDate == null) return ACTIVE; // không xác định thời hạn
        if (endDate.isBefore(today)) return EXPIRED;
        if (!endDate.isAfter(today.plusDays(EXPIRING_SOON_DAYS))) return EXPIRING_SOON;
        return ACTIVE;
    }
}