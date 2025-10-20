package rtp.example.rtp;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // Handle StockDataException with 503 Service Unavailable
    @ExceptionHandler(StockDataException.class)
    public ResponseEntity<Object> handleStockDataException(StockDataException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request);
        return new ResponseEntity<>(body, HttpStatus.SERVICE_UNAVAILABLE);
    }

    // Handle OptimisticLockingFailureException with 409 Conflict
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<Object> handleOptimisticLocking(OptimisticLockingFailureException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.CONFLICT,
                "Conflict occurred, please retry the operation.", request);
        logger.warn("Optimistic locking failure: ", ex);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    // Handle IllegalArgumentException - differentiate between access denied and validation
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        String message = ex.getMessage();

        // Check if it's an access denied error
        if (message != null && (message.contains("Access denied") ||
                message.contains("don't own") ||
                message.contains("can only access your own"))) {
            Map<String, Object> body = createErrorBody(HttpStatus.FORBIDDEN, message, request);
            logger.warn("Access denied: {}", message);
            return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
        }

        // Otherwise it's a validation error
        Map<String, Object> body = createErrorBody(HttpStatus.BAD_REQUEST, message, request);
        logger.warn("Illegal argument: ", ex);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Handle EntityNotFoundException with 404 Not Found
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.NOT_FOUND, ex.getMessage(), request);
        logger.warn("Entity not found: {}", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    // Handle Spring Security AccessDeniedException with 403 Forbidden
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Object> handleAccessDenied(AccessDeniedException ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.FORBIDDEN,
                "You don't have permission to access this resource.", request);
        logger.warn("Access denied: ", ex);
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    // Handle authentication exceptions with 401 Unauthorized
    @ExceptionHandler({AuthenticationException.class, BadCredentialsException.class})
    public ResponseEntity<Object> handleAuthenticationException(Exception ex, WebRequest request) {
        Map<String, Object> body = createErrorBody(HttpStatus.UNAUTHORIZED,
                "Authentication failed. Please check your credentials.", request);
        logger.warn("Authentication failed: ", ex);
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    // Handle validation errors (400 Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Object> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Validation Failed");

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );
        body.put("message", fieldErrors);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        logger.warn("Validation failed: {}", fieldErrors);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // Handle all other exceptions with generic message and logging
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleAllExceptions(Exception ex, WebRequest request) {
        logger.error("Unhandled exception: ", ex);

        Map<String, Object> body = createErrorBody(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please contact support.", request);

        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Helper method to create consistent error response bodies
    private Map<String, Object> createErrorBody(HttpStatus status, String message, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", request.getDescription(false).replace("uri=", ""));
        return body;
    }
}