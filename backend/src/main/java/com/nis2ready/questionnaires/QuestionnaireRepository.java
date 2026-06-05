package com.nis2ready.questionnaires;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface QuestionnaireRepository extends JpaRepository<Questionnaire, UUID> {
  Optional<Questionnaire> findFirstByTypeAndActiveTrueOrderByVersionDesc(QuestionnaireType type);
}
