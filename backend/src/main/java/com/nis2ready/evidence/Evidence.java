package com.nis2ready.evidence;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "evidence")
public class Evidence extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID organizationId;
  @Column(nullable = false)
  public String title;
  @Column(columnDefinition = "TEXT")
  public String description;
  public String category;
  public UUID linkedControlId;
  public UUID linkedTaskId;
  @Column(nullable = false)
  public UUID fileId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public EvidenceStatus status = EvidenceStatus.PENDING_REVIEW;
  public LocalDate validUntil;
  @Column(nullable = false)
  public UUID uploadedBy;
}
