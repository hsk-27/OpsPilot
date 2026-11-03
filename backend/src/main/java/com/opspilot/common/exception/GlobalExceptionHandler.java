package com.opspilot.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("API Exception [correlationId={}]: status={}, code={}, message={}",
                correlationId, ex.getStatus(), ex.getCode(), ex.getMessage());

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        pd.setTitle(ex.getStatus().getReasonPhrase());
        pd.setType(URI.create("https://opspilot.io/errors/" + ex.getCode().toLowerCase()));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", ex.getCode());
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(ex.getStatus()).body(pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fe.getField(), fe.getDefaultMessage());
        }

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        pd.setTitle("Bad Request");
        pd.setType(URI.create("https://opspilot.io/errors/validation-error"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", "VALIDATION_ERROR");
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("fieldErrors", fieldErrors);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        pd.setTitle("Unauthorized");
        pd.setType(URI.create("https://opspilot.io/errors/invalid-credentials"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", "INVALID_CREDENTIALS");
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied: insufficient permissions");
        pd.setTitle("Forbidden");
        pd.setType(URI.create("https://opspilot.io/errors/access-denied"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", "ACCESS_DENIED");
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneric(Exception ex, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("Unhandled Exception [correlationId={}]: {}", correlationId, ex.getMessage(), ex);

        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred");
        pd.setTitle("Internal Server Error");
        pd.setType(URI.create("https://opspilot.io/errors/internal-server-error"));
        pd.setInstance(URI.create(request.getRequestURI()));
        pd.setProperty("code", "INTERNAL_SERVER_ERROR");
        pd.setProperty("correlationId", correlationId);
        pd.setProperty("timestamp", Instant.now());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }
}
