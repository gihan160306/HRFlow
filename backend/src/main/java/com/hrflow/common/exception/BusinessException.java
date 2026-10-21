package com.hrflow.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Vi phạm quy tắc nghiệp vụ. Mang theo mã lỗi máy đọc được và HTTP status phù hợp:
 * <ul>
 *   <li>422 — dữ liệu không đạt quy tắc (vượt số dư phép, vượt giờ OT, insurance_salary &lt; lương tối thiểu vùng...)</li>
 *   <li>409 — xung đột trạng thái (kỳ lương LOCKED, duyệt đơn đã xử lý...)</li>
 *   <li>403 — vi phạm quy tắc phân quyền nghiệp vụ (tự duyệt đơn của mình, maker-checker adjustment)</li>
 * </ul>
 */
public class BusinessException extends RuntimeException {

    private final String code;
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException unprocessable(String code, String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, code, message);
    }

    public static BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }

    public static BusinessException forbidden(String code, String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, code, message);
    }

    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
