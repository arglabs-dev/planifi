package com.planifi.backend.api;

import com.planifi.backend.application.AccountNotFoundException;
import com.planifi.backend.application.ApiKeyNotFoundException;
import com.planifi.backend.application.EmailAlreadyRegisteredException;
import com.planifi.backend.application.IdempotencyKeyReuseException;
import com.planifi.backend.application.InvalidCredentialsException;
import com.planifi.backend.application.TagNotFoundException;
import com.planifi.backend.api.dto.ErrorResponse;
import io.micrometer.tracing.Tracer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private final Tracer tracer;

    public ApiExceptionHandler(Tracer tracer) {
        this.tracer = tracer;
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyRegistered(
            EmailAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("AUTH_EMAIL_IN_USE", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("AUTH_INVALID_CREDENTIALS", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(ApiKeyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleApiKeyNotFound(ApiKeyNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("API_KEY_NOT_FOUND", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("ACCOUNT_NOT_FOUND", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(IdempotencyKeyReuseException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyReuse(IdempotencyKeyReuseException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("IDEMPOTENCY_KEY_REUSED", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(TagNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTagNotFound(TagNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("TAG_NOT_FOUND", ex.getMessage(), traceId()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(ApiExceptionHandler::formatFieldError)
                .reduce((left, right) -> left + "; " + right)
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("VALIDATION_ERROR", message, traceId()));
    }

    private static String formatFieldError(FieldError error) {
        String field = error.getField();
        String message = error.getDefaultMessage();
        return field + ": " + message;
    }

    private String traceId() {
        if (tracer.currentSpan() == null) {
            return "unknown";
        }
        return tracer.currentSpan().context().traceId();
    }
}
