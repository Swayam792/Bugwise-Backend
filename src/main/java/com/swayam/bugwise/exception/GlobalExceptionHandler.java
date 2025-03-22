package com.swayam.bugwise.exception;

import com.swayam.bugwise.dto.ExceptionResponseDTO;
import com.swayam.bugwise.dto.ValidationErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errorMessages = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errorMessages.put(error.getField(), error.getDefaultMessage());
        }
        return new ResponseEntity<>(new ValidationErrorResponseDTO("Validation failed", errorMessages), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidationExceptions(ValidationException ex) {
        return new ResponseEntity<>(new ValidationErrorResponseDTO("Validation failed", ex.getErrors()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ExceptionResponseDTO> handleAuthenticationException(AuthenticationException ex) {
        ExceptionResponseDTO exceptionResponseDTO = new ExceptionResponseDTO("Incorrect username/password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(exceptionResponseDTO);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ExceptionResponseDTO> handleNoSuchElementException(NoSuchElementException ex) {
        ExceptionResponseDTO exceptionResponseDTO = new ExceptionResponseDTO("Resource not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponseDTO);
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ExceptionResponseDTO> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        ExceptionResponseDTO exceptionResponseDTO = new ExceptionResponseDTO(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(exceptionResponseDTO);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ExceptionResponseDTO> handleNoHandlerFoundException(NoHandlerFoundException ex) {
        ExceptionResponseDTO exceptionResponseDTO = new ExceptionResponseDTO("Endpoint not found");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(exceptionResponseDTO);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ExceptionResponseDTO> handleGenericException(Exception ex) {
        ex.printStackTrace();
        ExceptionResponseDTO exceptionResponseDTO = new ExceptionResponseDTO(ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(exceptionResponseDTO);
    }
}