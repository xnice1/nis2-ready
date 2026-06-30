package com.nis2ready.evidence;

import com.nis2ready.common.ApiException;
import com.nis2ready.audit.*;
import com.nis2ready.controls.ControlRepository;
import com.nis2ready.files.*;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.tasks.RemediationTaskRepository;
import com.nis2ready.users.Role;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.evidence.EvidenceDtos.*;

@Service
public class EvidenceService {
  private final EvidenceRepository evidence;
  private final StoredFileRepository files;
  private final FileStorageService storage;
  private final ControlRepository controls;
  private final RemediationTaskRepository tasks;
  private final AuditService audit;
  public EvidenceService(EvidenceRepository evidence, StoredFileRepository files, FileStorageService storage, ControlRepository controls, RemediationTaskRepository tasks, AuditService audit) {
    this.evidence = evidence; this.files = files; this.storage = storage; this.controls = controls; this.tasks = tasks; this.audit = audit;
  }
  @Transactional
  public EvidenceResponse upload(UUID orgId, UUID userId, Role role, String title, String description, String category, UUID controlId, UUID taskId, LocalDate validUntil, MultipartFile file) {
    RoleGuard.requireContributor(role);
    validateLinks(orgId, controlId, taskId);
    if (title == null || title.isBlank()) throw ApiException.badRequest("title is required");
    var stored = storage.store(orgId, userId, file);
    var e = new Evidence();
    e.organizationId = orgId; e.uploadedBy = userId; e.title = title; e.description = description; e.category = category;
    e.linkedControlId = controlId; e.linkedTaskId = taskId; e.fileId = stored.id; e.validUntil = validUntil;
    evidence.save(e);
    audit.record(orgId, userId, AuditEventType.EVIDENCE_UPLOADED, AuditOutcome.SUCCESS, "EVIDENCE", e.id, "Evidence uploaded",
      java.util.Map.of("title", e.title, "filename", stored.originalFilename, "contentType", stored.contentType, "sizeBytes", String.valueOf(stored.sizeBytes)));
    return toDto(e, stored);
  }
  public List<EvidenceResponse> list(UUID orgId) { return evidence.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().map(this::toDto).toList(); }
  public EvidenceResponse get(UUID orgId, UUID id) { return toDto(entity(orgId, id)); }
  @Transactional
  public EvidenceResponse status(UUID orgId, UUID userId, Role role, UUID id, EvidenceStatus status) {
    RoleGuard.requireContributor(role);
    if (status == null) throw ApiException.badRequest("status is required");
    var e = entity(orgId, id);
    var previous = e.status;
    e.status = status;
    audit.record(orgId, userId, AuditEventType.EVIDENCE_STATUS_CHANGED, AuditOutcome.SUCCESS, "EVIDENCE", id, "Evidence status changed",
      java.util.Map.of("from", previous.name(), "to", status.name()));
    return toDto(e);
  }
  @Transactional
  public void delete(UUID orgId, UUID userId, Role role, UUID id) {
    RoleGuard.requireContributor(role);
    var e = entity(orgId, id);
    var f = files.findByIdAndOrganizationId(e.fileId, orgId).orElseThrow(() -> ApiException.notFound("File not found"));
    evidence.delete(e);
    evidence.flush();
    storage.delete(orgId, f);
    audit.record(orgId, userId, AuditEventType.EVIDENCE_DELETED, AuditOutcome.SUCCESS, "EVIDENCE", id, "Evidence deleted",
      java.util.Map.of("title", e.title, "filename", f.originalFilename));
  }
  public Download download(UUID orgId, UUID userId, UUID id) {
    var e = entity(orgId, id);
    var f = files.findByIdAndOrganizationId(e.fileId, orgId).orElseThrow(() -> ApiException.notFound("File not found"));
    audit.record(orgId, userId, AuditEventType.EVIDENCE_DOWNLOADED, AuditOutcome.SUCCESS, "EVIDENCE", id, "Evidence downloaded",
      java.util.Map.of("title", e.title, "filename", f.originalFilename, "contentType", f.contentType));
    return new Download(storage.load(orgId, f), f.originalFilename, f.contentType);
  }
  private Evidence entity(UUID orgId, UUID id) { return evidence.findByIdAndOrganizationId(id, orgId).orElseThrow(() -> ApiException.notFound("Evidence not found")); }
  private EvidenceResponse toDto(Evidence e) { return toDto(e, files.findByIdAndOrganizationId(e.fileId, e.organizationId).orElseThrow(() -> ApiException.notFound("File not found"))); }
  private EvidenceResponse toDto(Evidence e, StoredFile f) {
    return new EvidenceResponse(e.id, e.title, e.description, e.category, e.linkedControlId, e.linkedTaskId, f.id, f.originalFilename, f.contentType, f.sizeBytes, f.checksumSha256, e.status, e.validUntil);
  }
  private void validateLinks(UUID orgId, UUID controlId, UUID taskId) {
    if (controlId != null && !controls.existsById(controlId)) throw ApiException.notFound("Control not found");
    if (taskId != null && tasks.findByIdAndOrganizationId(taskId, orgId).isEmpty()) throw ApiException.notFound("Task not found");
  }
  public record Download(Resource resource, String filename, String contentType) {}
}
