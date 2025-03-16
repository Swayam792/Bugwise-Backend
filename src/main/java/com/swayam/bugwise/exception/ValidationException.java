package com.swayam.bugwise.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ValidationException extends RuntimeException {
    private Map<String, String> errors;
}