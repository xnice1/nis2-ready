package com.nis2ready.policies;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "policy_templates")
public class PolicyTemplate {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false, unique = true)
  public String name;
  @Column(nullable = false)
  public String category;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String content;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String disclaimer;
}
