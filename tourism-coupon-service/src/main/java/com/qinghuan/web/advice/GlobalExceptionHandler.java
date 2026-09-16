package com.qinghuan.web.advice;

import com.qinghuan.common.exception.BusinessException;
import com.qinghuan.common.exception.ErrorCode;
import com.qinghuan.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 将应用异常转换成统一、安全的 HTTP 响应。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return response(statusOf(errorCode), errorCode, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return invalidRequest(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleHandlerMethodValidation(
            HandlerMethodValidationException exception) {
        String message = exception.getParameterValidationResults().stream()
                .flatMap(this::validationMessages)
                .distinct()
                .collect(Collectors.joining("; "));
        return invalidRequest(message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .distinct()
                .collect(Collectors.joining("; "));
        return invalidRequest(message);
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleMalformedRequest(Exception exception) {
        return invalidRequest("请求参数格式不正确");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception exception) {
        LOGGER.error("Unhandled application exception", exception);
        return response(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.getMessage()
        );
    }

    private ResponseEntity<ApiResponse<Void>> invalidRequest(String message) {
        String responseMessage = message == null || message.isBlank()
                ? ErrorCode.INVALID_REQUEST.getMessage()
                : message;
        return response(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_REQUEST, responseMessage);
    }

    private ResponseEntity<ApiResponse<Void>> response(
            HttpStatus status, ErrorCode errorCode, String message) {
        return ResponseEntity.status(status).body(ApiResponse.failure(errorCode, message));
    }

    private HttpStatus statusOf(ErrorCode errorCode) {
        return switch (errorCode) {
            case INVALID_REQUEST -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED, USER_NOT_FOUND -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case SUCCESS, INTERNAL_ERROR, GENERIC_ERROR -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    private Stream<String> validationMessages(ParameterValidationResult result) {
        if (result instanceof ParameterErrors errors) {
            return errors.getFieldErrors().stream()
                    .map(error -> error.getField() + ": " + error.getDefaultMessage());
        }
        return result.getResolvableErrors().stream()
                .map(error -> error.getDefaultMessage() == null
                        ? ErrorCode.INVALID_REQUEST.getMessage()
                        : error.getDefaultMessage());
    }
}
