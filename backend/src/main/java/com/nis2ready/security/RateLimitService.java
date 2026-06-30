package com.nis2ready.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {
  private final boolean enabled;
  private final int apiRequestsPerMinute;
  private final int authRequestsPerMinute;
  private final int loginFailuresPerWindow;
  private final Duration loginFailureWindow;
  private final ConcurrentHashMap<String, Counter> counters = new ConcurrentHashMap<>();

  public RateLimitService(@Value("${app.rate-limit.enabled:true}") boolean enabled,
                          @Value("${app.rate-limit.api-requests-per-minute:300}") int apiRequestsPerMinute,
                          @Value("${app.rate-limit.auth-requests-per-minute:20}") int authRequestsPerMinute,
                          @Value("${app.rate-limit.login-failures-per-window:5}") int loginFailuresPerWindow,
                          @Value("${app.rate-limit.login-failure-window-seconds:900}") long loginFailureWindowSeconds) {
    this.enabled = enabled;
    this.apiRequestsPerMinute = apiRequestsPerMinute;
    this.authRequestsPerMinute = authRequestsPerMinute;
    this.loginFailuresPerWindow = loginFailuresPerWindow;
    this.loginFailureWindow = Duration.ofSeconds(loginFailureWindowSeconds);
  }

  public RateLimitDecision checkApiRequest(String key) {
    return check("api", key, apiRequestsPerMinute, Duration.ofMinutes(1));
  }

  public RateLimitDecision checkAuthRequest(String ipAddress) {
    return check("auth", ipAddress, authRequestsPerMinute, Duration.ofMinutes(1));
  }

  public RateLimitDecision checkLoginAllowed(String email) {
    if (!enabled || loginFailuresPerWindow <= 0) return RateLimitDecision.allow();
    String key = loginFailureKey(email);
    Instant now = Instant.now();
    synchronized (this) {
      Counter counter = counters.get(key);
      if (counter == null || !now.isBefore(counter.resetAt)) {
        counters.remove(key);
        return RateLimitDecision.allow();
      }
      if (counter.count >= loginFailuresPerWindow) {
        return RateLimitDecision.reject(retryAfterSeconds(now, counter.resetAt));
      }
      return RateLimitDecision.allow();
    }
  }

  public void recordLoginFailure(String email) {
    if (!enabled || loginFailuresPerWindow <= 0) return;
    increment("login-failure", loginFailureKeyPart(email), loginFailuresPerWindow + 1, loginFailureWindow);
  }

  public void clearLoginFailures(String email) {
    counters.remove(loginFailureKey(email));
  }

  public String currentClientIp() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      return clientIp(attrs.getRequest());
    }
    return "unknown";
  }

  public String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) return realIp.trim();
    return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
  }

  public RateLimitDecision check(String namespace, String key, int maxRequests, Duration window) {
    if (!enabled || maxRequests <= 0) return RateLimitDecision.allow();
    return increment(namespace, key, maxRequests, window);
  }

  private RateLimitDecision increment(String namespace, String key, int maxRequests, Duration window) {
    Instant now = Instant.now();
    String counterKey = namespace + ":" + (key == null || key.isBlank() ? "unknown" : key);
    synchronized (this) {
      Counter counter = counters.get(counterKey);
      if (counter == null || !now.isBefore(counter.resetAt)) {
        counters.put(counterKey, new Counter(1, now.plus(window)));
        cleanupExpired(now);
        return RateLimitDecision.allow();
      }
      if (counter.count >= maxRequests) {
        return RateLimitDecision.reject(retryAfterSeconds(now, counter.resetAt));
      }
      counter.count++;
      return RateLimitDecision.allow();
    }
  }

  private String loginFailureKey(String email) {
    return "login-failure:" + loginFailureKeyPart(email);
  }

  private String loginFailureKeyPart(String email) {
    return normalizeEmail(email) + ":" + currentClientIp();
  }

  private String normalizeEmail(String email) {
    return email == null ? "unknown" : email.trim().toLowerCase(Locale.ROOT);
  }

  private long retryAfterSeconds(Instant now, Instant resetAt) {
    return Math.max(1, Duration.between(now, resetAt).toSeconds());
  }

  private void cleanupExpired(Instant now) {
    if (counters.size() < 10000) return;
    counters.entrySet().removeIf(entry -> !now.isBefore(entry.getValue().resetAt));
  }

  private static final class Counter {
    private int count;
    private final Instant resetAt;

    private Counter(int count, Instant resetAt) {
      this.count = count;
      this.resetAt = resetAt;
    }
  }

  public record RateLimitDecision(boolean allowed, long retryAfterSeconds) {
    static RateLimitDecision allow() {
      return new RateLimitDecision(true, 0);
    }

    static RateLimitDecision reject(long retryAfterSeconds) {
      return new RateLimitDecision(false, retryAfterSeconds);
    }
  }
}
