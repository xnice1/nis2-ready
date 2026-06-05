package com.nis2ready.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
  private final SecretKey key;
  private final String issuer;
  public JwtService(@Value("${app.jwt-secret}") String secret, @Value("${app.jwt-issuer:nis2-ready}") String issuer) {
    if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
      throw new IllegalStateException("JWT_SECRET must be at least 32 bytes for HS256");
    }
    this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.issuer = issuer;
  }
  public String create(UUID userId, UUID organizationId) {
    if (organizationId == null) throw new IllegalArgumentException("organizationId is required");
    Instant now = Instant.now();
    var builder = Jwts.builder().issuer(issuer).subject(userId.toString()).issuedAt(Date.from(now));
    builder.claim("org", organizationId.toString());
    return builder
      .expiration(Date.from(now.plusSeconds(60 * 60 * 12))).signWith(key).compact();
  }
  public JwtClaims parse(String token) {
    var claims = Jwts.parser().requireIssuer(issuer).verifyWith(key).build().parseSignedClaims(token).getPayload();
    var userId = UUID.fromString(claims.getSubject());
    var orgClaim = claims.get("org", String.class);
    if (orgClaim == null) throw new IllegalArgumentException("JWT organization claim is required");
    return new JwtClaims(userId, UUID.fromString(orgClaim));
  }
  public record JwtClaims(UUID userId, UUID organizationId) {}
}
