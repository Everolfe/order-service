package com.github.everolfe.orderservice.exception;

import com.github.everolfe.orderservice.dto.GetErrorDto;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<GetErrorDto> entityNotFound(
            final EntityNotFoundException e,
            final WebRequest request) {
        return buildErrorResponse(
                e.getMessage(),
                HttpStatus.NOT_FOUND,
                request
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<GetErrorDto> runtimeException(
            final RuntimeException e,
            final WebRequest request) {
        return buildErrorResponse(
                e.getMessage(),
                HttpStatus.SERVICE_UNAVAILABLE,
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<GetErrorDto> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {

        String message = "Database conflict: " + ex.getMostSpecificCause().getMessage();


        return buildErrorResponse(
                "A resource with these details already exists or violates data constraints.",
                HttpStatus.CONFLICT,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GetErrorDto> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        return buildErrorResponse(
                errorMessage,
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GetErrorDto> handleMalformedJson(
            HttpMessageNotReadableException ex, WebRequest request) {

        return buildErrorResponse(
                "Invalid request body: JSON is malformed.",
                HttpStatus.BAD_REQUEST,
                request
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<GetErrorDto> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        return buildErrorResponse(
                "Authentication failed: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<GetErrorDto> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        return buildErrorResponse(
                "Access Denied: You do not have permission to access this resource.",
                HttpStatus.FORBIDDEN,
                request
        );
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<GetErrorDto> handleFeignStatusException(
            FeignException ex, WebRequest request) {

        return buildErrorResponse(
                String.format(
                        "External service error [%d]: %s",
                        ex.status(),
                        ex.getMessage() != null ? ex.getMessage() : "No details"
                ),
                ex.status() > 0 ?
                        HttpStatus.valueOf(ex.status()) :
                        HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<GetErrorDto> handleCircuitBreakerOpen(
            FeignException ex, WebRequest request) {

        return buildErrorResponse(
                "Service Unavailable: Circuit breaker is open - User Service is temporarily unavailable",
                HttpStatus.SERVICE_UNAVAILABLE,
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GetErrorDto> handleGlobalException(
            Exception ex, WebRequest request) {


        return buildErrorResponse(
                "An unexpected internal server error occurred.",
                HttpStatus.INTERNAL_SERVER_ERROR,
                request
        );
    }

    private ResponseEntity<GetErrorDto> buildErrorResponse(
            String message, HttpStatus status, WebRequest request){
        GetErrorDto errorDto = new GetErrorDto(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getDescription(false).replace("uri=", "")
        );
        return new ResponseEntity<>(errorDto, status);
    }
}
