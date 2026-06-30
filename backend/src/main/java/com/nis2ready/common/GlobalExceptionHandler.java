package com.nis2ready.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.Instant;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(ApiException.class)
  ResponseEntity<ApiError> api(ApiException ex, HttpServletRequest request) {
    var response = error(ex.getStatus(), ex.getMessage(), request);
    if (ex.getRetryAfterSeconds() != null) {
      return ResponseEntity.status(response.getStatusCode())
        .header("Retry-After", String.valueOf(ex.getRetryAfterSeconds()))
        .body(response.getBody());
    }
    return response;
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
    String msg = ex.getBindingResult().getFieldErrors().stream()
      .map(e -> e.getField() + " " + e.getDefaultMessage()).collect(Collectors.joining("; "));
    return error(HttpStatus.BAD_REQUEST, msg, request);
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  @SuppressWarnings("unused")
  ResponseEntity<ApiError> large(MaxUploadSizeExceededException ex, HttpServletRequest request) {
    return error(HttpStatus.PAYLOAD_TOO_LARGE, "File too large", request);
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MultipartException.class})
  ResponseEntity<ApiError> badRequest(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  @SuppressWarnings("unused")
  ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "Duplicate value or invalid related record", request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  @SuppressWarnings("unused")
  ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest request) {
    return error(HttpStatus.FORBIDDEN, "Forbidden", request);
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiError> generic(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), request);
  }

  private ResponseEntity<ApiError> error(HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message, request.getRequestURI()));
  }
}
