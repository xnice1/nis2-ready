package com.nis2ready.organizations;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class Organization extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public String name;
  public String country;
  public String sector;
  public String employeeCountRange;
  public String annualTurnoverRange;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public OrganizationType organizationType = OrganizationType.COMPANY;
}
