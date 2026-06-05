package com.nis2ready.assessments;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssessmentAnswerRepository extends JpaRepository<AssessmentAnswer, UUID> {
  List<AssessmentAnswer> findByAssessmentId(UUID assessmentId);
  Optional<AssessmentAnswer> findByAssessmentIdAndControlId(UUID assessmentId, UUID controlId);
}
