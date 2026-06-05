package com.nis2ready.security;

import com.nis2ready.organizations.Organization;
import com.nis2ready.users.Role;
import com.nis2ready.users.User;
import java.util.UUID;

public record AppUserPrincipal(UUID userId, String email, UUID organizationId, Role role) {
  public static AppUserPrincipal of(User user, Organization organization, Role role) {
    return new AppUserPrincipal(user.id, user.email, organization.id, role);
  }
}
