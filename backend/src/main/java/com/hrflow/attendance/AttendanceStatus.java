package com.hrflow.attendance;

/** Trạng thái công theo ngày (data_dictionary.md, bảng attendance). */
public enum AttendanceStatus {
    /** Đã check-in, chưa check-out. */
    OPEN,
    /** Đủ công. */
    PRESENT,
    /** Thiếu giờ vượt dung sai, chờ HR quyết định trước khi chốt bảng công. */
    EXCEPTION,
    /** Vắng không phép. */
    ABSENT,
    /** Nghỉ có đơn đã duyệt. */
    LEAVE,
    /** Làm từ xa / gặp khách hàng có Manager xác nhận. */
    REMOTE
}