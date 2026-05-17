package com.briefy.global.error;

import com.briefy.global.dto.ApiResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BriefyException.class)
    public ResponseEntity<ApiResponse<?>> handleBriefyException(BriefyException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case SCHEDULE_NOT_FOUND, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case UNAUTHORIZED, INVALID_TOKEN, INVALID_CREDENTIALS -> HttpStatus.UNAUTHORIZED;
            case USER_ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case SCHEDULE_OVERLAP -> HttpStatus.CONFLICT;
            case SCHEDULE_INVALID_TIME -> HttpStatus.BAD_REQUEST;
            default -> HttpStatus.BAD_REQUEST;
        };
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getErrorCode()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<?>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(FieldError::getDefaultMessage)
            .collect(Collectors.joining(", "));
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<?>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest()
            .body(ApiResponse.error(ex.getMessage() != null ? ex.getMessage() : "잘못된 요청입니다"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
        String msg = ex.getMessage();
        if (msg != null && msg.contains("schedules_no_overlap")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(BriefyErrorCode.SCHEDULE_OVERLAP));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiResponse.error("데이터 제약 조건 위반"));
    }
}
