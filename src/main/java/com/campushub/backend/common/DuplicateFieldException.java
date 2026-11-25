package com.campushub.backend.common;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DuplicateFieldException extends ResponseStatusException {

    private final Map<String, String> errors;

    public DuplicateFieldException(String field, String message) {
        super(HttpStatus.CONFLICT, message);
        this.errors = Map.of(field, message);
    }

    public Map<String, String> getErrors() {
        return errors;
    }
}
