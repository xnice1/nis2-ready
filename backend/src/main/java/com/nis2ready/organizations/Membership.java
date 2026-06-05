package com.nis2ready.organizations;

import com.nis2ready.common.AuditedEntity;
import com.nis2ready.users.Role;
import com.nis2ready.users.User;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "memberships", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "organization_id"}))
public class Membership extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @ManyToOne(optional = false)
  @JoinColumn(name = "user_id")
  public User user;
  @ManyToOne(optional = false)
  @JoinColumn(name = "organization_id")
  public Organization organization;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public Role role;
}
