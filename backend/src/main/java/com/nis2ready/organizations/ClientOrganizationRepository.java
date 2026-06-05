package com.nis2ready.organizations;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ClientOrganizationRepository extends JpaRepository<ClientOrganization, UUID> {
  List<ClientOrganization> findByConsultancyOrganizationId(UUID consultancyOrganizationId);
}
