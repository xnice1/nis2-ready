package com.nis2ready.tasks;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "remediation_tasks")
public class RemediationTask extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID organizationId;
  @Column(nullable = false)
  public String title;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String description;
  @Column(nullable = false)
  public String category;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public TaskPriority priority = TaskPriority.MEDIUM;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public TaskStatus status = TaskStatus.TODO;
  public UUID ownerUserId;
  public LocalDate dueDate;
  public UUID relatedControlId;
  public UUID relatedAssessmentId;
}
