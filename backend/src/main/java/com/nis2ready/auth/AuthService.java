package com.nis2ready.auth;

import com.nis2ready.common.ApiException;
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
  public AuthService(UserRepository users, OrganizationRepository organizations, MembershipRepository memberships, PasswordEncoder encoder, JwtService jwt) {
    this.users = users; this.organizations = organizations; this.memberships = memberships; this.encoder = encoder; this.jwt = jwt;
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
    return new AuthResponse(jwt.create(user.id, org.id), meResponse(user, org, Role.OWNER));
  }
  public AuthResponse login(LoginRequest request) {
    var user = users.findByEmailIgnoreCase(normalizeEmail(request.email())).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
    if (!encoder.matches(request.password(), user.passwordHash)) throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    var membership = memberships.findFirstByUserOrderByCreatedAtAsc(user).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "No organization membership"));
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
