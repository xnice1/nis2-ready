package com.nis2ready.incidents;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "incident_actions")
public class IncidentAction {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @ManyToOne(optional = false)
  @JoinColumn(name = "incident_id")
  public Incident incident;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String description;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public IncidentActionStatus status = IncidentActionStatus.TODO;
  @Column(nullable = false)
  public UUID createdBy;
  @Column(nullable = false)
  public Instant createdAt = Instant.now();
  public Instant completedAt;
}
