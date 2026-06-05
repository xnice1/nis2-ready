package com.nis2ready.evidence;

import java.time.LocalDate;
import java.util.UUID;

public class EvidenceDtos {
  public record EvidenceResponse(UUID id, String title, String description, String category, UUID linkedControlId,
                                 UUID linkedTaskId, UUID fileId, String originalFilename, String contentType,
                                 long sizeBytes, String checksumSha256, EvidenceStatus status, LocalDate validUntil) {}
  public record EvidenceStatusRequest(EvidenceStatus status) {}
}
