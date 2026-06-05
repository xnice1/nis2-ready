package com.nis2ready.assessments;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentRepository extends JpaRepository<Assessment, UUID> {
  List<Assessment> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
  Optional<Assessment> findFirstByOrganizationIdAndStatusOrderByCompletedAtDesc(UUID organizationId, AssessmentStatus status);
}
