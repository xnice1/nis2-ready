package com.nis2ready.organizations;

import com.nis2ready.users.Role;
import java.util.UUID;

public class OrganizationDtos {
  public record OrganizationResponse(UUID id, String name, String country, String sector, String employeeCountRange,
                                     String annualTurnoverRange, OrganizationType organizationType) {}
  public record UpdateOrganizationRequest(String name, String country, String sector, String employeeCountRange,
                                          String annualTurnoverRange, OrganizationType organizationType) {}
  public record MemberResponse(UUID userId, String email, String firstName, String lastName, String fullName, Role role) {}
  public record InviteMemberRequest(String email, Role role) {}
}
