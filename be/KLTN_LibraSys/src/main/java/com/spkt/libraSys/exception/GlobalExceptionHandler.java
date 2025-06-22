package com.spkt.libraSys.exception;

import com.fasterxml.jackson.core.JsonParseException;
import com.spkt.libraSys.service.user.UserException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.DeserializationException;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MIN_ATTRIBUTE = "min";

    /**
     * Handle general exceptions (RuntimeException)
     */
    @ExceptionHandler(value = RuntimeException.class)
    public ResponseEntity<ApiResponse> handlingRuntimeException(RuntimeException exception) {
        log.error("Exception: ", exception);
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setSuccess(false);
        apiResponse.setCode(ErrorCode.UNKNOWN_ERROR.getCode());
        apiResponse.setMessage(ErrorCode.UNKNOWN_ERROR.getMessage());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(apiResponse);
    }
    @ExceptionHandler(value = {JwtException.class,MalformedJwtException.class, DeserializationException.class})
    public ResponseEntity<ApiResponse> jwtException(Exception exception) {
        log.error("JWT exception");
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setMessage("Token được cung cấp không đúng định dạng.");
        apiResponse.setSuccess(false);
        apiResponse.setCode(ErrorCode.INVALID_REQUEST.getCode());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponse);
    }

    /**
     * Handle JSON format exceptions (HttpMessageNotReadableException)
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setSuccess(false);
        apiResponse.setCode(ErrorCode.INVALID_REQUEST.getCode());
        apiResponse.setMessage("Chuyển đổi JSON không đúng định dạng.");
        return ResponseEntity.badRequest().body(apiResponse);
    }
    /**
     * Handle application exceptions (AppException)
     */
    @ExceptionHandler(value = AppException.class)
    public ResponseEntity<ApiResponse> handlingAppException(AppException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.error("Handling AppException: {}", exception.getMessage());
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setSuccess(false);
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(exception.getMessage());

        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);

    }

    /**
     * Handle access denied exceptions (AccessDeniedException)
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handlingAccessDeniedException(AccessDeniedException exception) {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;

        return ResponseEntity.status(errorCode.getStatusCode())
                .body(ApiResponse.builder()
                        .code(errorCode.getCode())
                        .message(errorCode.getMessage())
                        .build());
    }

    /**
     * Handle validation errors in request (MethodArgumentNotValidException)
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handlingValidation(MethodArgumentNotValidException exception) {
        // Initialize default error code
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;

        // Create Map to store validation errors
        Map<String, String> errors = new HashMap<>();

        // Iterate through all validation errors
        exception.getBindingResult().getAllErrors().forEach((error) -> {
            if (error instanceof FieldError) {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            }
        });

        // Create ApiResponse with error code and error details
        ApiResponse<Map<String, String>> apiResponse = ApiResponse.<Map<String, String>>builder()
                .code(errorCode.getCode())
                .message("Đã xảy ra lỗi xác thực")
                .data(errors)
                .build();

        return ResponseEntity.badRequest().body(apiResponse);
    }

    /**
     * Helper function to replace attributes in error message
     */
    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));
        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }

    /**
     * Handle entity not found exceptions (EntityNotFoundException)
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.RESOURCE_NOT_FOUND.getCode());
        apiResponse.setMessage(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponse);
    }

    /**
     * Handle data-related exceptions (IllegalArgumentException)
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        ApiResponse apiResponse = new ApiResponse();

        apiResponse.setCode(ErrorCode.INVALID_REQUEST.getCode());
        apiResponse.setMessage("Dữ liệu không hợp lệ: " + ex.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<String>> handleRequestNotPermitted(RequestNotPermitted e) {
        log.error("Rate limit exceeded: {}", e.getMessage());
        ApiResponse<String> response = ApiResponse.<String>builder()
                .code(429)
                .message("Yêu cầu đã vượt quá giới hạn cho phép. Vui lòng thử lại sau.")
                .build();
        return ResponseEntity.status(429).body(response);
    }
}
