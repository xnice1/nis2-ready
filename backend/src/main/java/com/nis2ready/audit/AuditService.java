package com.nis2ready.audit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.users.Role;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.nis2ready.audit.AuditDtos.*;

@Service
public class AuditService {
  private static final TypeReference<Map<String, String>> DETAILS_TYPE = new TypeReference<>() {};
  private final AuditEventRepository events;
  private final ObjectMapper objectMapper;

  public AuditService(AuditEventRepository events, ObjectMapper objectMapper) {
    this.events = events;
    this.objectMapper = objectMapper;
  }

  @Transactional
  public void record(UUID organizationId, UUID actorUserId, AuditEventType type, AuditOutcome outcome,
                     String targetType, UUID targetId, String summary, Map<String, String> details) {
    var event = new AuditEvent();
    event.organizationId = organizationId;
    event.actorUserId = actorUserId;
    event.eventType = type;
    event.outcome = outcome;
    event.targetType = targetType;
    event.targetId = targetId;
    event.summary = truncate(summary, 500);
    event.detailsJson = writeDetails(details);
    applyRequestMetadata(event);
    events.save(event);
  }

  @Transactional(readOnly = true)
  public List<AuditEventResponse> list(UUID organizationId, Role actorRole, int limit) {
    RoleGuard.requireOrganizationManager(actorRole);
    int safeLimit = Math.max(1, Math.min(limit, 200));
    return events.findByOrganizationIdOrderByCreatedAtDesc(organizationId, PageRequest.of(0, safeLimit))
      .stream().map(this::toDto).toList();
  }

  private AuditEventResponse toDto(AuditEvent event) {
    return new AuditEventResponse(event.id, event.createdAt, event.actorUserId, event.eventType, event.outcome,
      event.targetType, event.targetId, event.summary, readDetails(event.detailsJson), event.ipAddress, event.userAgent);
  }

  private String writeDetails(Map<String, String> details) {
    try {
      return objectMapper.writeValueAsString(sanitizeDetails(details == null ? Map.of() : details));
    } catch (Exception e) {
      return "{}";
    }
  }

  private Map<String, String> readDetails(String detailsJson) {
    try {
      return objectMapper.readValue(detailsJson == null || detailsJson.isBlank() ? "{}" : detailsJson, DETAILS_TYPE);
    } catch (Exception e) {
      return Map.of();
    }
  }

  private Map<String, String> sanitizeDetails(Map<String, String> details) {
    var sanitized = new LinkedHashMap<String, String>();
    details.forEach((key, value) -> {
      if (key != null && value != null) sanitized.put(truncate(key, 80), truncate(value, 300));
    });
    return sanitized;
  }

  private void applyRequestMetadata(AuditEvent event) {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      HttpServletRequest request = attrs.getRequest();
      event.ipAddress = truncate(request.getRemoteAddr(), 64);
      event.userAgent = truncate(request.getHeader("User-Agent"), 500);
    }
  }

  private String truncate(String value, int max) {
    if (value == null) return null;
    return value.length() > max ? value.substring(0, max) : value;
  }
}
