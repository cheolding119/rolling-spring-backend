package com.rolling.api.global.exception;

import com.rolling.api.global.logging.LogMdcKeys;
import com.rolling.api.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthException(AuthException e) {
        applyErrorContext(HttpStatus.UNAUTHORIZED, e.getCode());
        log.error("AuthException: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        applyErrorContext(e.getHttpStatus(), e.getCode());
        log.error("BusinessException: {} - {}", e.getCode(), e.getMessage());
        return ResponseEntity
                .status(e.getHttpStatus())
                .body(ApiResponse.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<ApiResponse<Void>> handleRestClientException(RestClientException e) {
        applyErrorContext(HttpStatus.BAD_GATEWAY, "EXTERNAL_API_ERROR");
        log.error("RestClientException: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_GATEWAY)
                .body(ApiResponse.error("EXTERNAL_API_ERROR", "외부 API 호출 중 오류가 발생했습니다"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다");

        applyErrorContext(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        log.error("ValidationException: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        applyErrorContext(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR");
        log.error("Unexpected exception: ", e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다"));
    }

    private void applyErrorContext(HttpStatus status, String errorCode) {
        MDC.put(LogMdcKeys.STATUS, Integer.toString(status.value()));
        MDC.put(LogMdcKeys.ERROR_CODE, errorCode);
    }
}
