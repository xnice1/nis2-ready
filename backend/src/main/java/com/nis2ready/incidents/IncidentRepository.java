package com.nis2ready.incidents;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentRepository extends JpaRepository<Incident, UUID> {
  List<Incident> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
  Optional<Incident> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
