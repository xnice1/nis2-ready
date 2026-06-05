package com.nis2ready.policies;

import com.nis2ready.common.ApiException;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.users.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.policies.PolicyDtos.*;

@Service
public class PolicyService {
  private final PolicyTemplateRepository templates;
  private final OrganizationPolicyRepository policies;
  public PolicyService(PolicyTemplateRepository templates, OrganizationPolicyRepository policies) {
    this.templates = templates; this.policies = policies;
  }
  public List<PolicyTemplate> templates() { return templates.findAllByOrderByNameAsc(); }
  public PolicyTemplate template(UUID id) { return templates.findById(id).orElseThrow(() -> ApiException.notFound("Policy template not found")); }
  @Transactional
  public OrganizationPolicy fromTemplate(UUID orgId, Role role, UUID templateId) {
    RoleGuard.requireContributor(role);
    var t = template(templateId);
    var p = new OrganizationPolicy();
    p.organizationId = orgId; p.templateId = t.id; p.title = t.name; p.content = t.content + "\n\n" + t.disclaimer;
    return policies.save(p);
  }
  public List<OrganizationPolicy> list(UUID orgId) { return policies.findByOrganizationIdOrderByUpdatedAtDesc(orgId); }
  public OrganizationPolicy get(UUID orgId, UUID id) { return policies.findByIdAndOrganizationId(id, orgId).orElseThrow(() -> ApiException.notFound("Policy not found")); }
  @Transactional
  public OrganizationPolicy update(UUID orgId, Role role, UUID id, PolicyUpdateRequest request) {
    RoleGuard.requireContributor(role);
    var p = get(orgId, id);
    if (request.title() != null) p.title = request.title();
    if (request.content() != null) p.content = request.content();
    if (request.status() != null) p.status = request.status();
    return p;
  }
}
