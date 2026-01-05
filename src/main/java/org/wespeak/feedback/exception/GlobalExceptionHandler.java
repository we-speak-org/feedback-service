package org.wespeak.feedback.exception;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(ResourceNotFoundException.class)
  public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
    log.error("Resource not found: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
  }

  @ExceptionHandler(ForbiddenException.class)
  public ResponseEntity<Map<String, Object>> handleForbidden(ForbiddenException ex) {
    log.error("Access forbidden: {}", ex.getMessage());
    return buildErrorResponse(HttpStatus.FORBIDDEN, ex.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
    log.error("Unexpected error", ex);
    return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error");
  }

  private ResponseEntity<Map<String, Object>> buildErrorResponse(
      HttpStatus status, String message) {
    Map<String, Object> error = new HashMap<>();
    error.put("timestamp", Instant.now().toString());
    error.put("status", status.value());
    error.put("error", status.getReasonPhrase());
    error.put("message", message);
    return ResponseEntity.status(status).body(error);
  }
}
