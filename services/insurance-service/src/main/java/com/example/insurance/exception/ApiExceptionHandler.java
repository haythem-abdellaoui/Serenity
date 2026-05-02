package com.example.insurance.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleResponseStatus(ResponseStatusException ex) {
        String msg = ex.getReason() != null ? ex.getReason() : "Request error";
        return ResponseEntity.status(ex.getStatusCode()).body(Map.of("message", msg));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Map<String, String>> handleBindException(BindException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : (error.getField() + " is invalid"))
                .collect(Collectors.joining("; "));
        if (msg.isBlank()) {
            msg = "Validation failed";
        }
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, String>> handleConstraintViolation(ConstraintViolationException ex) {
        String msg = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));
        if (msg.isBlank()) {
            msg = "Validation failed";
        }
        return ResponseEntity.badRequest().body(Map.of("message", msg));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String field = ex.getName() != null ? ex.getName() : "Request parameter";
        return ResponseEntity.badRequest().body(Map.of("message", field + " has an invalid format"));
    }

    @ExceptionHandler(OcrMajorMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleOcrMajorMismatch(OcrMajorMismatchException ex) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("message", ex.getMessage());
        payload.put("severity", "MAJOR");
        payload.put("mismatches", ex.getMismatches());
        return ResponseEntity.badRequest().body(payload);
    }
}
