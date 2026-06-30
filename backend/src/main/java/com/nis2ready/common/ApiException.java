package com.nis2ready.common;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
  private final HttpStatus status;
  private final Long retryAfterSeconds;
  public ApiException(HttpStatus status, String message) {
    super(message);
    this.status = status;
    this.retryAfterSeconds = null;
  }
  public ApiException(HttpStatus status, String message, Long retryAfterSeconds) {
    super(message);
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
  }
  public HttpStatus getStatus() {
    return status;
  }
  public Long getRetryAfterSeconds() {
    return retryAfterSeconds;
  }
  public static ApiException notFound(String message) {
    return new ApiException(HttpStatus.NOT_FOUND, message);
  }
  public static ApiException badRequest(String message) {
    return new ApiException(HttpStatus.BAD_REQUEST, message);
  }
  public static ApiException forbidden(String message) {
    return new ApiException(HttpStatus.FORBIDDEN, message);
  }
  public static ApiException tooManyRequests(String message) {
    return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message);
  }
  public static ApiException tooManyRequests(String message, long retryAfterSeconds) {
    return new ApiException(HttpStatus.TOO_MANY_REQUESTS, message, Long.valueOf(retryAfterSeconds));
  }
}
