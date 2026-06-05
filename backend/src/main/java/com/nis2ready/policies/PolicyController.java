package com.nis2ready.policies;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.policies.PolicyDtos.*;

@RestController
public class PolicyController {
  private final PolicyService service;
  public PolicyController(PolicyService service) { this.service = service; }
  @GetMapping("/api/policy-templates")
  List<PolicyTemplate> templates() { return service.templates(); }
  @GetMapping("/api/policy-templates/{id}")
  PolicyTemplate template(@PathVariable UUID id) { return service.template(id); }
  @PostMapping("/api/policies/from-template/{templateId}")
  OrganizationPolicy fromTemplate(Authentication auth, @PathVariable UUID templateId) {
    var user = CurrentUser.get(auth);
    return service.fromTemplate(user.organizationId(), user.role(), templateId);
  }
  @GetMapping("/api/policies")
  List<OrganizationPolicy> policies(Authentication auth) { return service.list(CurrentUser.get(auth).organizationId()); }
  @GetMapping("/api/policies/{id}")
  OrganizationPolicy policy(Authentication auth, @PathVariable UUID id) { return service.get(CurrentUser.get(auth).organizationId(), id); }
  @PutMapping("/api/policies/{id}")
  OrganizationPolicy update(Authentication auth, @PathVariable UUID id, @RequestBody PolicyUpdateRequest request) {
    var user = CurrentUser.get(auth);
    return service.update(user.organizationId(), user.role(), id, request);
  }
}
