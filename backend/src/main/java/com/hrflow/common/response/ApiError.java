package com.hrflow.common.response;

import java.time.Instant;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Định dạng lỗi chuẩn, khớp schema ApiError trong openapi_spec.yaml:
 * code, message, details[{field, message}], timestamp.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        String code,
        String message,
        List<FieldDetail> details,
        Instant timestamp) {

    public record FieldDetail(String field, String message) {
    }

    public static ApiError of(String code, String message) {
        return new ApiError(code, message, List.of(), Instant.now());
    }

    public static ApiError of(String code, String message, List<FieldDetail> details) {
        return new ApiError(code, message, details, Instant.now());
    }
}
