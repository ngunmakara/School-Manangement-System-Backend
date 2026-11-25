package com.campushub.backend.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException exception) {

        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage() == null
                            ? "Invalid value"
                            : fieldError.getDefaultMessage()
            );
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(errorBody("Validation failed", errors));
    }

    @ExceptionHandler(DuplicateFieldException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateField(
            DuplicateFieldException exception) {

        String message = exception.getReason() == null
                ? "The request could not be completed"
                : exception.getReason();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorBody(message, exception.getErrors()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(
            DataIntegrityViolationException exception) {

        Throwable cause = exception.getMostSpecificCause();
        String detail = cause.getMessage() == null
                ? "" : cause.getMessage().toLowerCase(Locale.ROOT);

        Map<String, String> errors = new LinkedHashMap<>();
        if (detail.contains("student_id")) {
            errors.put("studentId", "Student ID already exists");
        }
        if (detail.contains("email")) {
            errors.put("email", "Email already exists");
        }

        String message = errors.isEmpty()
                ? "The request conflicts with an existing record"
                : "Duplicate student field";

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(errorBody(message, errors));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleResponseStatus(
            ResponseStatusException exception) {

        String message = exception.getReason() == null
                ? "The request could not be completed"
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(errorBody(message, Map.of()));
    }

    private Map<String, Object> errorBody(
            String message, Map<String, String> errors) {

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("message", message);
        body.put("errors", errors);
        return body;
    }
}
