package com.nis2ready.security;

import com.nis2ready.common.ApiException;
import com.nis2ready.users.Role;

public final class RoleGuard {
  private RoleGuard() {}

  public static void requireOrganizationManager(Role role) {
    if (role != Role.OWNER && role != Role.ADMIN) {
      throw ApiException.forbidden("Only organization owners and admins can perform this action");
    }
  }

  public static void requireContributor(Role role) {
    if (role == Role.VIEWER) {
      throw ApiException.forbidden("Viewers have read-only access");
    }
  }
}
