package com.nis2ready.auth;

import com.nis2ready.common.ApiException;
import com.nis2ready.audit.*;
import com.nis2ready.organizations.*;
import com.nis2ready.security.JwtService;
import com.nis2ready.users.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

import static com.nis2ready.auth.AuthDtos.*;

@Service
public class AuthService {
  private final UserRepository users;
  private final OrganizationRepository organizations;
  private final MembershipRepository memberships;
  private final PasswordEncoder encoder;
  private final JwtService jwt;
  private final AuditService audit;
  public AuthService(UserRepository users, OrganizationRepository organizations, MembershipRepository memberships, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
    this.users = users; this.organizations = organizations; this.memberships = memberships; this.encoder = encoder; this.jwt = jwt; this.audit = audit;
  }
  @Transactional
  public AuthResponse register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (users.existsByEmailIgnoreCase(email)) throw ApiException.badRequest("duplicate email");
    var user = new User();
    user.email = email;
    user.firstName = request.firstName().trim();
    user.lastName = request.lastName().trim();
    user.passwordHash = encoder.encode(request.password());
    users.save(user);
    var org = new Organization();
    org.name = request.organizationName().trim();
    org.country = request.country();
    org.sector = request.sector();
    org.employeeCountRange = request.employeeCountRange();
    org.annualTurnoverRange = request.annualTurnoverRange();
    org.organizationType = request.organizationType() == null ? OrganizationType.COMPANY : request.organizationType();
    organizations.save(org);
    var membership = new Membership();
    membership.user = user; membership.organization = org; membership.role = Role.OWNER;
    memberships.save(membership);
    audit.record(org.id, user.id, AuditEventType.USER_REGISTERED, AuditOutcome.SUCCESS, "USER", user.id, "User registered and organization created",
      java.util.Map.of("email", user.email, "organizationName", org.name, "role", Role.OWNER.name()));
    return new AuthResponse(jwt.create(user.id, org.id), meResponse(user, org, Role.OWNER));
  }
  public AuthResponse login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    var user = users.findByEmailIgnoreCase(email).orElse(null);
    if (user == null) {
      audit.record(null, null, AuditEventType.LOGIN_FAILED, AuditOutcome.FAILURE, "USER", null, "Login failed",
        java.util.Map.of("email", email, "reason", "invalid_credentials"));
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
    if (!encoder.matches(request.password(), user.passwordHash)) {
      var membership = memberships.findFirstByUserOrderByCreatedAtAsc(user).orElse(null);
      audit.record(membership == null ? null : membership.organization.id, user.id, AuditEventType.LOGIN_FAILED, AuditOutcome.FAILURE, "USER", user.id, "Login failed",
        java.util.Map.of("email", email, "reason", "invalid_credentials"));
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }
    var membership = memberships.findFirstByUserOrderByCreatedAtAsc(user).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No organization membership"));
    audit.record(membership.organization.id, user.id, AuditEventType.LOGIN_SUCCEEDED, AuditOutcome.SUCCESS, "USER", user.id, "Login succeeded",
      java.util.Map.of("email", user.email, "role", membership.role.name()));
    return new AuthResponse(jwt.create(user.id, membership.organization.id), meResponse(user, membership.organization, membership.role));
  }
  public MeResponse me(java.util.UUID userId, java.util.UUID organizationId) {
    var user = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
    var membership = memberships.findByUser_IdAndOrganization_Id(user.id, organizationId).orElseThrow(() -> ApiException.notFound("Membership not found"));
    return meResponse(user, membership.organization, membership.role);
  }
  private MeResponse meResponse(User user, Organization org, Role role) {
    String fullName = user.firstName + " " + user.lastName;
    return new MeResponse(user.id, user.email, user.firstName, user.lastName, fullName, org.id, org.name, role);
  }
  private String normalizeEmail(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
