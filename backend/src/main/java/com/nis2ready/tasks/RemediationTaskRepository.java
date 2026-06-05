package com.nis2ready.tasks;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RemediationTaskRepository extends JpaRepository<RemediationTask, UUID> {
  List<RemediationTask> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
  Optional<RemediationTask> findByIdAndOrganizationId(UUID id, UUID organizationId);
  long countByOrganizationIdAndStatusNot(UUID organizationId, TaskStatus status);
  long countByOrganizationIdAndPriorityInAndStatusNot(UUID organizationId, List<TaskPriority> priorities, TaskStatus status);
  List<RemediationTask> findByOrganizationIdAndStatusAndUpdatedAtAfter(UUID organizationId, TaskStatus status, Instant after);
}
