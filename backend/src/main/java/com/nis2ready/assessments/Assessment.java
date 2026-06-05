package com.nis2ready.assessments;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "assessments")
public class Assessment extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID organizationId;
  public UUID questionnaireId;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AssessmentStatus status = AssessmentStatus.IN_PROGRESS;
  public Integer overallScore;
  @Enumerated(EnumType.STRING)
  public RiskLevel riskLevel;
  @Column(nullable = false)
  public Instant startedAt = Instant.now();
  public Instant completedAt;
  public UUID createdBy;
}
