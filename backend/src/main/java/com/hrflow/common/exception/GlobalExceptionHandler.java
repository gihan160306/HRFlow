package com.hrflow.common.exception;

import java.util.List;

import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.hrflow.common.response.ApiError;
import com.hrflow.common.response.ApiError.FieldDetail;

import jakarta.persistence.PersistenceException;

/**
 * Chuyển mọi exception thành {@link ApiError} theo openapi_spec.yaml.
 *
 * <p>Quy ước status (permission_matrix.md §3.4): 401 chưa xác thực, 403 không có quyền,
 * 404 không tìm thấy, 409 xung đột trạng thái/dữ liệu (kể cả kỳ lương LOCKED), 422 dữ liệu không hợp lệ.
 * Không ghi mật khẩu, token, CCCD, số tài khoản vào log.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** SQLSTATE do trigger CSDL ném khi sửa dữ liệu kỳ lương đã khóa/chốt công (database_design.md §6.3). */
    private static final String SQLSTATE_PERIOD_LOCKED = "55006";
    private static final String SQLSTATE_UNIQUE_VIOLATION = "23505";
    private static final String SQLSTATE_FK_VIOLATION = "23503";
    private static final String SQLSTATE_CHECK_VIOLATION = "23514";
    private static final String SQLSTATE_EXCLUSION_VIOLATION = "23P01";

    // ---------- Nghiệp vụ ----------

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiError> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(ex.getStatus()).body(ApiError.of(ex.getCode(), ex.getMessage()));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of("NOT_FOUND", ex.getMessage()));
    }

    // ---------- Validation (422) ----------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleBodyValidation(MethodArgumentNotValidException ex) {
        List<FieldDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return validationError("Dữ liệu đầu vào không hợp lệ", details);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleParamValidation(jakarta.validation.ConstraintViolationException ex) {
        List<FieldDetail> details = ex.getConstraintViolations().stream()
                .map(v -> new FieldDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return validationError("Dữ liệu đầu vào không hợp lệ", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        return validationError("Nội dung request không đúng định dạng JSON hoặc sai kiểu dữ liệu", List.of());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        return validationError("Thiếu tham số bắt buộc",
                List.of(new FieldDetail(ex.getParameterName(), "Tham số bắt buộc")));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return validationError("Tham số không hợp lệ",
                List.of(new FieldDetail(ex.getName(), "Giá trị không đúng kiểu dữ liệu")));
    }

    private ResponseEntity<ApiError> validationError(String message, List<FieldDetail> details) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of("VALIDATION_ERROR", message, details));
    }

    // ---------- Xác thực / phân quyền (401, 403) ----------

    @ExceptionHandler({BadCredentialsException.class, AuthenticationCredentialsNotFoundException.class})
    public ResponseEntity<ApiError> handleBadCredentials(RuntimeException ex) {
        // Thông báo chung, không tiết lộ tài khoản có tồn tại hay không
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("UNAUTHORIZED", "Thông tin đăng nhập không hợp lệ"));
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ApiError> handleAccountState(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("ACCOUNT_UNAVAILABLE", "Tài khoản bị khóa hoặc đã ngừng hoạt động"));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("FORBIDDEN", "Bạn không có quyền thực hiện thao tác này"));
    }

    // ---------- Ràng buộc CSDL (409 / 422) ----------

    @ExceptionHandler({DataIntegrityViolationException.class, PersistenceException.class})
    public ResponseEntity<ApiError> handleDatabase(Exception ex) {
        String sqlState = extractSqlState(ex);
        if (sqlState != null) {
            switch (sqlState) {
                case SQLSTATE_PERIOD_LOCKED:
                    return conflict("PERIOD_LOCKED", "Kỳ lương đã chốt/khóa, không được sửa hoặc xóa dữ liệu. "
                            + "Hãy tạo adjustment ở kỳ kế tiếp.");
                case SQLSTATE_UNIQUE_VIOLATION:
                    return conflict("DUPLICATE_RESOURCE", "Dữ liệu bị trùng với bản ghi đã tồn tại");
                case SQLSTATE_EXCLUSION_VIOLATION:
                    return conflict("OVERLAPPING_PERIOD", "Khoảng thời gian hiệu lực bị chồng lấn với bản ghi khác");
                case SQLSTATE_FK_VIOLATION:
                    return conflict("REFERENCE_VIOLATION", "Dữ liệu đang được tham chiếu hoặc tham chiếu không tồn tại");
                case SQLSTATE_CHECK_VIOLATION:
                    return validationError("Dữ liệu vi phạm ràng buộc nghiệp vụ", List.of());
                default:
                    break;
            }
        }
        log.error("Lỗi CSDL không xác định (sqlState={})", sqlState, ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Đã xảy ra lỗi hệ thống"));
    }

    private ResponseEntity<ApiError> conflict(String code, String message) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(code, message));
    }

    /** Lần theo chuỗi cause để lấy SQLSTATE của PostgreSQL. */
    private String extractSqlState(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof ConstraintViolationException hibernateEx && hibernateEx.getSQLState() != null) {
                return hibernateEx.getSQLState();
            }
            if (current instanceof java.sql.SQLException sqlEx && sqlEx.getSQLState() != null) {
                return sqlEx.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }

    // ---------- Còn lại (500) ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("Lỗi không xác định", ex);
        // Không trả stack trace hay chi tiết nội bộ ra client
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "Đã xảy ra lỗi hệ thống"));
    }
}
