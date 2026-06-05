package com.nis2ready.policies;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrganizationPolicyRepository extends JpaRepository<OrganizationPolicy, UUID> {
  List<OrganizationPolicy> findByOrganizationIdOrderByUpdatedAtDesc(UUID organizationId);
  Optional<OrganizationPolicy> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
