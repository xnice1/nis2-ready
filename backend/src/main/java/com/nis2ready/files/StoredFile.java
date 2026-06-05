package com.nis2ready.files;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "stored_files")
public class StoredFile extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false)
  public UUID organizationId;
  @Column(nullable = false)
  public String originalFilename;
  @Column(nullable = false)
  public String storedFilename;
  @Column(nullable = false)
  public String storagePath;
  @Column(nullable = false)
  public String contentType;
  @Column(nullable = false)
  public long sizeBytes;
  @Column(nullable = false)
  public String checksumSha256;
  @Column(nullable = false)
  public UUID uploadedBy;
}
