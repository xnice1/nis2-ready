package com.nis2ready.organizations;

import com.nis2ready.users.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MembershipRepository extends JpaRepository<Membership, UUID> {
  Optional<Membership> findFirstByUserOrderByCreatedAtAsc(User user);
  Optional<Membership> findByUser_IdAndOrganization_Id(UUID userId, UUID organizationId);
  List<Membership> findByOrganizationId(UUID organizationId);
}
