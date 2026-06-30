package com.nis2ready.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Instant;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
  private final RateLimitService rateLimits;

  public RateLimitFilter(RateLimitService rateLimits) {
    this.rateLimits = rateLimits;
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain) throws ServletException, IOException {
    if (!"OPTIONS".equalsIgnoreCase(request.getMethod())) {
      var decision = decisionFor(request);
      if (!decision.allowed()) {
        writeRateLimitResponse(request, response, decision.retryAfterSeconds());
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private RateLimitService.RateLimitDecision decisionFor(HttpServletRequest request) {
    String path = request.getRequestURI();
    if ("/api/auth/login".equals(path) || "/api/auth/register".equals(path)) {
      return rateLimits.checkAuthRequest(rateLimits.clientIp(request));
    }
    if (path != null && path.startsWith("/api/")) {
      return rateLimits.checkApiRequest(apiKey(request));
    }
    return RateLimitService.RateLimitDecision.allow();
  }

  private String apiKey(HttpServletRequest request) {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof AppUserPrincipal user) {
      return "user:" + user.organizationId() + ":" + user.userId();
    }
    return "ip:" + rateLimits.clientIp(request);
  }

  private void writeRateLimitResponse(HttpServletRequest request, HttpServletResponse response, long retryAfterSeconds) throws IOException {
    response.setStatus(429);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,\"error\":\"Too Many Requests\",\"message\":\"Too many requests. Try again later.\",\"path\":\"" + request.getRequestURI() + "\"}");
  }
}
