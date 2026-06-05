package com.nis2ready.auth;

import com.nis2ready.organizations.OrganizationType;
import com.nis2ready.users.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public class AuthDtos {
  public record RegisterRequest(@Email @NotBlank String email, @NotBlank @Size(min = 8) String password,
                                @NotBlank String firstName, @NotBlank String lastName,
                                @NotBlank String organizationName, String country, String sector,
                                String employeeCountRange, String annualTurnoverRange, OrganizationType organizationType) {}
  public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) {}
  public record AuthResponse(String token, MeResponse user) {}
  public record MeResponse(UUID id, String email, String firstName, String lastName, String fullName,
                           UUID organizationId, String organizationName, Role role) {}
}
