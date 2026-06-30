package com.nis2ready.audit;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public class AuditDtos {
  public record AuditEventResponse(UUID id, Instant createdAt, UUID actorUserId, AuditEventType eventType,
                                   AuditOutcome outcome, String targetType, UUID targetId, String summary,
                                   Map<String, String> details, String ipAddress, String userAgent) {}
}
