package com.nis2ready.organizations;

import com.nis2ready.common.ApiException;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.users.Role;
import com.nis2ready.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import static com.nis2ready.organizations.OrganizationDtos.*;

@Service
public class OrganizationService {
  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final UserRepository users;
  public OrganizationService(OrganizationRepository organizations, MembershipRepository memberships, UserRepository users) {
    this.organizations = organizations; this.memberships = memberships; this.users = users;
  }
  public OrganizationResponse current(UUID orgId) {
    return toDto(organizations.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization not found")));
  }
  @Transactional
  public OrganizationResponse update(UUID orgId, Role actorRole, UpdateOrganizationRequest request) {
    RoleGuard.requireOrganizationManager(actorRole);
    var org = organizations.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization not found"));
    if (request.name() == null || request.name().isBlank()) throw ApiException.badRequest("organization name is required");
    org.name = request.name().trim();
    org.country = request.country();
    org.sector = request.sector();
    org.employeeCountRange = request.employeeCountRange();
    org.annualTurnoverRange = request.annualTurnoverRange();
    if (request.organizationType() != null) org.organizationType = request.organizationType();
    return toDto(org);
  }
  public List<MemberResponse> members(UUID orgId) {
    return memberships.findByOrganizationId(orgId).stream()
      .map(m -> new MemberResponse(m.user.id, m.user.email, m.user.firstName, m.user.lastName, m.user.firstName + " " + m.user.lastName, m.role)).toList();
  }
  @Transactional
  public MemberResponse invite(UUID orgId, Role actorRole, InviteMemberRequest request) {
    RoleGuard.requireOrganizationManager(actorRole);
    if (request.email() == null || request.email().isBlank()) throw ApiException.badRequest("email is required");
    if (request.role() == Role.OWNER && actorRole != Role.OWNER) throw ApiException.forbidden("Only owners can invite another owner");
    String email = request.email().trim().toLowerCase(Locale.ROOT);
    var user = users.findByEmailIgnoreCase(email).orElseThrow(() -> ApiException.notFound("User must register before they can be invited"));
    if (memberships.findByOrganizationId(orgId).stream().anyMatch(m -> m.user.id.equals(user.id))) {
      throw ApiException.badRequest("User is already a member of this organization");
    }
    var org = organizations.findById(orgId).orElseThrow(() -> ApiException.notFound("Organization not found"));
    var membership = new Membership();
    membership.user = user;
    membership.organization = org;
    membership.role = request.role() == null ? Role.VIEWER : request.role();
    memberships.save(membership);
    return new MemberResponse(user.id, user.email, user.firstName, user.lastName, user.firstName + " " + user.lastName, membership.role);
  }
  static OrganizationResponse toDto(Organization o) {
    return new OrganizationResponse(o.id, o.name, o.country, o.sector, o.employeeCountRange, o.annualTurnoverRange, o.organizationType);
  }
}
