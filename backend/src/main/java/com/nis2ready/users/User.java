package com.nis2ready.users;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false, unique = true)
  public String email;
  @Column(nullable = false)
  public String passwordHash;
  @Column(nullable = false)
  public String firstName;
  @Column(nullable = false)
  public String lastName;
  @Column(nullable = false)
  public boolean enabled = true;
}
