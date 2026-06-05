package com.nis2ready.policies;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PolicyTemplateRepository extends JpaRepository<PolicyTemplate, UUID> {
  List<PolicyTemplate> findAllByOrderByNameAsc();
}
