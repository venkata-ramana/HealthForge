package dev.healthforge.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiErrorHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        var status = HttpStatus.valueOf(exception.getStatusCode().value());
        return ResponseEntity.status(status).body(error(
                status,
                request,
                exception.getReason() != null ? exception.getReason() : status.getReasonPhrase(),
                List.of()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST,
                request,
                "Request validation failed.",
                exception.getBindingResult().getFieldErrors().stream()
                        .map(this::formatFieldError)
                        .toList()
        ));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(error(
                HttpStatus.BAD_REQUEST,
                request,
                "Request validation failed.",
                exception.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).toList()
        ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception exception, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                request,
                "Unexpected server error.",
                List.of()
        ));
    }

    private ApiErrorResponse error(HttpStatus status, HttpServletRequest request, String detail, List<String> errors) {
        var requestId = (String) request.getAttribute(RequestIdFilter.REQUEST_ID_ATTRIBUTE);
        return new ApiErrorResponse(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                detail,
                request.getRequestURI(),
                requestId,
                errors
        );
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + (error.getDefaultMessage() == null ? "invalid value" : error.getDefaultMessage());
    }
}
