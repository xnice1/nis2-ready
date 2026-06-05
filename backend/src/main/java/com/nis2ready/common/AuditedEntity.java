package com.nis2ready.common;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.Instant;

@MappedSuperclass
public abstract class AuditedEntity {
  @Column(nullable = false, updatable = false)
  public Instant createdAt;
  @Column(nullable = false)
  public Instant updatedAt;
  @PrePersist
  void prePersist() {
    createdAt = Instant.now();
    updatedAt = createdAt;
  }
  @PreUpdate
  void preUpdate() {
    updatedAt = Instant.now();
  }
}
