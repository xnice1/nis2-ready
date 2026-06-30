package com.nis2ready.audit;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "audit_events")
public class AuditEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  public UUID organizationId;
  public UUID actorUserId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AuditEventType eventType;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AuditOutcome outcome;
  public String targetType;
  public UUID targetId;
  @Column(nullable = false, length = 500)
  public String summary;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String detailsJson = "{}";
  @Column(length = 64)
  public String ipAddress;
  @Column(length = 500)
  public String userAgent;
  @Column(nullable = false, updatable = false)
  public Instant createdAt;

  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
  }
}
