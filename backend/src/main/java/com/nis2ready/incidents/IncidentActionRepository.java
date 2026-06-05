package com.nis2ready.incidents;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncidentActionRepository extends JpaRepository<IncidentAction, UUID> {
  List<IncidentAction> findByIncidentId(UUID incidentId);
  Optional<IncidentAction> findByIdAndIncidentId(UUID id, UUID incidentId);
}
