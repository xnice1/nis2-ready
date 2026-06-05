package com.nis2ready.incidents;

import com.nis2ready.common.ApiException;
import com.nis2ready.organizations.MembershipRepository;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.users.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.incidents.IncidentDtos.*;

@Service
public class IncidentService {
  private final IncidentRepository incidents;
  private final IncidentActionRepository actions;
  private final MembershipRepository memberships;
  public IncidentService(IncidentRepository incidents, IncidentActionRepository actions, MembershipRepository memberships) {
    this.incidents = incidents; this.actions = actions; this.memberships = memberships;
  }
  public List<Incident> list(UUID orgId) { return incidents.findByOrganizationIdOrderByCreatedAtDesc(orgId); }
  public IncidentDetail get(UUID orgId, UUID id) { var i = incident(orgId, id); return new IncidentDetail(i, actions.findByIncidentId(i.id)); }
  @Transactional
  public IncidentDetail create(UUID orgId, UUID userId, Role role, IncidentRequest request) {
    RoleGuard.requireContributor(role);
    var i = new Incident(); i.organizationId = orgId; apply(i, request); incidents.save(i);
    if (Boolean.TRUE.equals(request.generateDefaultActions())) {
      List.of("Assign incident owner", "Record detection time", "Identify affected systems", "Contain the incident", "Preserve evidence", "Notify management", "Assess whether external reporting may be required.", "Prepare internal communication", "Perform post-incident review")
        .forEach(text -> addAction(i, userId, new IncidentActionRequest(text, IncidentActionStatus.TODO)));
    }
    return get(orgId, i.id);
  }
  @Transactional
  public IncidentDetail update(UUID orgId, Role role, UUID id, IncidentRequest request) {
    RoleGuard.requireContributor(role);
    var i = incident(orgId, id); apply(i, request); return get(orgId, id);
  }
  @Transactional
  public IncidentAction addAction(UUID orgId, UUID incidentId, UUID userId, Role role, IncidentActionRequest request) {
    RoleGuard.requireContributor(role);
    return addAction(incident(orgId, incidentId), userId, request);
  }
  @Transactional
  public IncidentAction updateAction(UUID orgId, Role role, UUID incidentId, UUID actionId, IncidentActionRequest request) {
    RoleGuard.requireContributor(role);
    incident(orgId, incidentId);
    var a = actions.findByIdAndIncidentId(actionId, incidentId).orElseThrow(() -> ApiException.notFound("Incident action not found"));
    if (request.description() != null) a.description = request.description();
    if (request.status() != null) { a.status = request.status(); a.completedAt = request.status() == IncidentActionStatus.DONE ? Instant.now() : null; }
    return a;
  }
  private IncidentAction addAction(Incident incident, UUID userId, IncidentActionRequest request) {
    var a = new IncidentAction(); a.incident = incident; a.createdBy = userId; a.description = request.description();
    if (request.status() != null) a.status = request.status();
    return actions.save(a);
  }
  private Incident incident(UUID orgId, UUID id) { return incidents.findByIdAndOrganizationId(id, orgId).orElseThrow(() -> ApiException.notFound("Incident not found")); }
  private void apply(Incident i, IncidentRequest r) {
    if (r.title() == null || r.title().isBlank()) throw ApiException.badRequest("title is required");
    if (r.ownerUserId() != null && memberships.findByOrganizationId(i.organizationId).stream().noneMatch(m -> m.user.id.equals(r.ownerUserId()))) {
      throw ApiException.badRequest("owner user must be a member of this organization");
    }
    i.title = r.title(); i.description = r.description();
    if (r.severity() != null) i.severity = r.severity();
    if (r.status() != null) i.status = r.status();
    i.detectedAt = r.detectedAt(); i.reportedInternallyAt = r.reportedInternallyAt(); i.affectedSystems = r.affectedSystems(); i.ownerUserId = r.ownerUserId();
  }
}
