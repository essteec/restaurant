package com.ste.restaurant.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message(ex.getMessage())
                .error(ex.getError())
                .status(ex.getStatus())
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail(ex.getDetail())
                .build();

        return ResponseEntity.status(ex.getStatus()).body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Validation failed")
                .error("VALIDATION_ERROR")
                .status(400)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .errors(errors)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        Class<?> requiredType = ex.getRequiredType();
        String expectedType = requiredType != null ? requiredType.getSimpleName() : "unknown";
        String message = String.format("Invalid value '%s' for parameter '%s'. Expected type: %s", 
                ex.getValue(), ex.getName(), expectedType);
        
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Invalid parameter type")
                .error("TYPE_MISMATCH")
                .status(400)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail(message)
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Malformed JSON request")
                .error("INVALID_REQUEST_BODY")
                .status(400)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail("Request body is missing or malformed")
                .build();

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Authentication required")
                .error("AUTHENTICATION_REQUIRED")
                .status(401)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail("Valid authentication credentials are required to access this resource")
                .build();

        return ResponseEntity.status(401).body(errorResponse);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Access denied")
                .error("ACCESS_DENIED")
                .status(403)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail("You do not have permission to access this resource")
                .build();

        return ResponseEntity.status(403).body(errorResponse);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Access denied")
                .error("ACCESS_DENIED")
                .status(403)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail("You do not have permission to access this resource")
                .build();

        return ResponseEntity.status(403).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .message("Internal server error")
                .error("INTERNAL_ERROR")
                .status(500)
                .timestamp(LocalDateTime.now())
                .path(request.getRequestURI())
                .detail("An unexpected error occurred")
                .build();

        return ResponseEntity.status(500).body(errorResponse);
    }
}
