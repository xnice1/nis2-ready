package com.nis2ready.security;

import com.nis2ready.common.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;

public final class CurrentUser {
  private CurrentUser() {}
  public static AppUserPrincipal get(Authentication authentication) {
    if (authentication == null || !(authentication.getPrincipal() instanceof AppUserPrincipal principal)) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }
    return principal;
  }
}
