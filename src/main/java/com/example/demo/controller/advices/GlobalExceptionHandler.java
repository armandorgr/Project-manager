package com.example.demo.controller.advices;

import com.example.demo.controller.responses.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response<String>> handleResponseStatus(ResponseStatusException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, ex.getStatusCode());
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<Response<String>> handleDuplicateKey(DuplicateKeyException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<Response<String>> handleUsernameNotFound(UsernameNotFoundException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Response<String>> handleBadCredentials(BadCredentialsException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        Response<Map<String, String>> response = new Response<>("ERROR", ex.getMessage(), errors, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Response<String>> handleNoSuchElementException(NoSuchElementException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Response<String>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Response<String>> handleNoBodyException(HttpMessageNotReadableException ex) {
        Response<String> response = new Response<>("ERROR", ex.getMessage(), null, null);
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
