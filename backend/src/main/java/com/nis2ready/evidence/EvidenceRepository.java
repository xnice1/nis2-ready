package com.nis2ready.evidence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EvidenceRepository extends JpaRepository<Evidence, UUID> {
  List<Evidence> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
  Optional<Evidence> findByIdAndOrganizationId(UUID id, UUID organizationId);
  long countByOrganizationId(UUID organizationId);
  long countByOrganizationIdAndStatus(UUID organizationId, EvidenceStatus status);
  List<Evidence> findByOrganizationIdAndValidUntilBefore(UUID organizationId, LocalDate date);
}
