package com.nis2ready.tasks;

import com.nis2ready.assessments.AnswerOption;
import com.nis2ready.assessments.AssessmentAnswer;
import com.nis2ready.assessments.AssessmentRepository;
import com.nis2ready.common.ApiException;
import com.nis2ready.controls.ControlRepository;
import com.nis2ready.organizations.MembershipRepository;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.users.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.tasks.TaskDtos.*;

@Service
public class TaskService {
  private final RemediationTaskRepository tasks;
  private final AssessmentRepository assessments;
  private final ControlRepository controls;
  private final MembershipRepository memberships;
  public TaskService(RemediationTaskRepository tasks, AssessmentRepository assessments, ControlRepository controls, MembershipRepository memberships) {
    this.tasks = tasks; this.assessments = assessments; this.controls = controls; this.memberships = memberships;
  }
  public List<RemediationTask> list(UUID orgId) { return tasks.findByOrganizationIdOrderByCreatedAtDesc(orgId); }
  public RemediationTask get(UUID orgId, UUID id) { return tasks.findByIdAndOrganizationId(id, orgId).orElseThrow(() -> ApiException.notFound("Task not found")); }
  @Transactional
  public RemediationTask create(UUID orgId, Role role, TaskRequest request) {
    RoleGuard.requireContributor(role);
    var task = new RemediationTask();
    task.organizationId = orgId;
    apply(orgId, task, request);
    return tasks.save(task);
  }
  @Transactional
  public RemediationTask update(UUID orgId, Role role, UUID id, TaskRequest request) {
    RoleGuard.requireContributor(role);
    var task = get(orgId, id);
    apply(orgId, task, request);
    return task;
  }
  @Transactional
  public void delete(UUID orgId, Role role, UUID id) {
    RoleGuard.requireContributor(role);
    tasks.delete(get(orgId, id));
  }
  @Transactional
  public void generateForAssessment(UUID orgId, UUID assessmentId, List<AssessmentAnswer> answers) {
    answers.stream().filter(a -> a.answer == AnswerOption.NO || a.answer == AnswerOption.UNKNOWN || a.answer == AnswerOption.PARTIAL)
      .forEach(a -> {
        var task = new RemediationTask();
        task.organizationId = orgId;
        task.relatedAssessmentId = assessmentId;
        task.relatedControlId = a.controlId;
        task.title = a.questionText;
        task.description = a.recommendedAction + "\nRecommended evidence: " + a.recommendedEvidence;
        task.category = a.category;
        task.priority = priority(a.weight, a.answer);
        tasks.save(task);
      });
  }
  private TaskPriority priority(int weight, AnswerOption answer) {
    if (answer == AnswerOption.NO || answer == AnswerOption.UNKNOWN) return weight >= 5 ? TaskPriority.CRITICAL : TaskPriority.HIGH;
    return weight >= 5 ? TaskPriority.HIGH : TaskPriority.MEDIUM;
  }
  private void apply(UUID orgId, RemediationTask task, TaskRequest request) {
    if (request.title() == null || request.title().isBlank()) throw ApiException.badRequest("title is required");
    if (request.description() == null || request.description().isBlank()) throw ApiException.badRequest("description is required");
    if (request.category() == null || request.category().isBlank()) throw ApiException.badRequest("category is required");
    validateReferences(orgId, request);
    task.title = request.title();
    task.description = request.description();
    task.category = request.category();
    if (request.priority() != null) task.priority = request.priority();
    if (request.status() != null) task.status = request.status();
    task.ownerUserId = request.ownerUserId();
    task.dueDate = request.dueDate();
    task.relatedControlId = request.relatedControlId();
    task.relatedAssessmentId = request.relatedAssessmentId();
  }
  private void validateReferences(UUID orgId, TaskRequest request) {
    if (request.ownerUserId() != null && memberships.findByOrganizationId(orgId).stream().noneMatch(m -> m.user.id.equals(request.ownerUserId()))) {
      throw ApiException.badRequest("owner user must be a member of this organization");
    }
    if (request.relatedControlId() != null && !controls.existsById(request.relatedControlId())) {
      throw ApiException.notFound("Control not found");
    }
    if (request.relatedAssessmentId() != null) {
      var assessment = assessments.findById(request.relatedAssessmentId()).orElseThrow(() -> ApiException.notFound("Assessment not found"));
      if (!assessment.organizationId.equals(orgId)) throw ApiException.forbidden("Assessment belongs to another organization");
    }
  }
}
