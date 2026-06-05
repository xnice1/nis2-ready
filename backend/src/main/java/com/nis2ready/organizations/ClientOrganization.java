package com.nis2ready.organizations;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "client_organizations", uniqueConstraints = @UniqueConstraint(columnNames = {"consultancy_organization_id", "client_organization_id"}))
public class ClientOrganization extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID consultancyOrganizationId;
  @Column(nullable = false)
  public UUID clientOrganizationId;
}
