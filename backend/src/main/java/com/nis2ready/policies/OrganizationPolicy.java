package com.nis2ready.policies;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "organization_policies")
public class OrganizationPolicy extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID organizationId;
  public UUID templateId;
  @Column(nullable = false)
  public String title;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String content;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public PolicyStatus status = PolicyStatus.DRAFT;
}
