package com.nis2ready.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
  List<AuditEvent> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId, Pageable pageable);
}
