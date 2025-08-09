package com.web_game.common.Exception;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.web_game.common.DTO.Respone.ApiResponse;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    // ✅ Xử lý exception tùy chỉnh
    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // ✅ Xử lý lỗi không đủ quyền
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // ✅ Xử lý lỗi validation (DTO @Valid)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handlingValidation(MethodArgumentNotValidException exception) {
        String enumKey = exception.getFieldError().getDefaultMessage();
        ErrorCode errorCode = ErrorCode.INVALID_KEY;
        Map<String, Object> attributes = null;

        try {
            errorCode = ErrorCode.valueOf(enumKey);

            List<ObjectError> errors = exception.getBindingResult().getAllErrors();
            ConstraintViolation<?> constraintViolation = errors.get(0).unwrap(ConstraintViolation.class);
            attributes = constraintViolation.getConstraintDescriptor().getAttributes();

            log.info("Constraint attributes: {}", attributes);
        } catch (IllegalArgumentException e) {
            log.warn("Mã lỗi không hợp lệ: {}", enumKey);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý validation: ", e);
        }

        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(
                        Objects.nonNull(attributes)
                                ? mapAttribute(errorCode.getMessage(), attributes)
                                : errorCode.getMessage()
                )
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // ✅ Xử lý lỗi nghiêm trọng không thuộc exception (ví dụ StackOverflowError)
    @ExceptionHandler(Error.class)
    public ResponseEntity<ApiResponse> handleFatalError(Error error) {
        System.err.println("Caught fatal error: " + error.getClass().getName());

        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message("Lỗi hệ thống nghiêm trọng.")
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // ✅ Xử lý mọi exception còn lại (không match các handler trên)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handlingAllException(Exception exception) {
        // Tránh log vòng lặp nếu cause là StackOverflowError
        Throwable root = getRootCause(exception);
        if (root instanceof StackOverflowError) {
            System.err.println("Đã phát hiện StackOverflowError, bỏ qua log sâu");
        } else {
            log.error("Lỗi không xác định: ", exception);
        }

        ErrorCode errorCode = ErrorCode.UNCATEGORIZED_EXCEPTION;
        ApiResponse apiResponse = ApiResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // ✅ Lấy nguyên nhân sâu nhất
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        return (cause == null || cause == throwable) ? throwable : getRootCause(cause);
    }

    // ✅ Thay {min} trong thông báo bằng giá trị cụ thể
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
