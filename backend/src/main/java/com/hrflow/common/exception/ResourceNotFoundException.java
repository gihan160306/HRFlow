package com.hrflow.common.exception;

/**
 * Không tìm thấy tài nguyên → HTTP 404.
 *
 * <p>Với hồ sơ nhạy cảm, có thể ném exception này thay cho 403 để không lộ sự tồn tại
 * của bản ghi (permission_matrix.md §3.4).
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " không tồn tại: " + id);
    }
}
