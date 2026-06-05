package com.nis2ready.incidents;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incidents")
public class Incident extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID organizationId;
  @Column(nullable = false)
  public String title;
  @Column(columnDefinition = "TEXT")
  public String description;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public IncidentSeverity severity = IncidentSeverity.MEDIUM;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public IncidentStatus status = IncidentStatus.OPEN;
  public Instant detectedAt;
  public Instant reportedInternallyAt;
  @Column(columnDefinition = "TEXT")
  public String affectedSystems;
  public UUID ownerUserId;
}
