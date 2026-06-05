package com.nis2ready.organizations;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.nis2ready.organizations.OrganizationDtos.*;

@RestController
@RequestMapping("/api/organizations")
public class OrganizationController {
  private final OrganizationService service;
  public OrganizationController(OrganizationService service) { this.service = service; }
  @GetMapping("/current")
  OrganizationResponse current(Authentication auth) { return service.current(CurrentUser.get(auth).organizationId()); }
  @PutMapping("/current")
  OrganizationResponse update(Authentication auth, @RequestBody UpdateOrganizationRequest request) {
    var user = CurrentUser.get(auth);
    return service.update(user.organizationId(), user.role(), request);
  }
  @GetMapping("/members")
  List<MemberResponse> members(Authentication auth) { return service.members(CurrentUser.get(auth).organizationId()); }
  @PostMapping("/invite")
  MemberResponse invite(Authentication auth, @RequestBody InviteMemberRequest request) {
    var user = CurrentUser.get(auth);
    return service.invite(user.organizationId(), user.role(), request);
  }
}
