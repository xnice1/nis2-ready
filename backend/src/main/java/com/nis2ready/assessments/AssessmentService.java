package com.nis2ready.assessments;

import com.nis2ready.common.ApiException;
import com.nis2ready.controls.ControlRepository;
import com.nis2ready.questionnaires.QuestionnaireRepository;
import com.nis2ready.questionnaires.QuestionnaireType;
import com.nis2ready.security.RoleGuard;
import com.nis2ready.users.Role;
import com.nis2ready.tasks.TaskService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.assessments.AssessmentDtos.*;

@Service
public class AssessmentService {
  private final AssessmentRepository assessments;
  private final AssessmentAnswerRepository answers;
  private final ControlRepository controls;
  private final AssessmentScoringService scoring;
  private final TaskService tasks;
  private final QuestionnaireRepository questionnaires;
  public AssessmentService(AssessmentRepository assessments, AssessmentAnswerRepository answers, ControlRepository controls, AssessmentScoringService scoring, TaskService tasks, QuestionnaireRepository questionnaires) {
    this.assessments = assessments; this.answers = answers; this.controls = controls; this.scoring = scoring; this.tasks = tasks; this.questionnaires = questionnaires;
  }
  @Transactional
  public AssessmentResponse create(UUID orgId, UUID userId, Role role) {
    RoleGuard.requireContributor(role);
    var a = new Assessment(); a.organizationId = orgId;
    a.createdBy = userId;
    questionnaires.findFirstByTypeAndActiveTrueOrderByVersionDesc(QuestionnaireType.READINESS).ifPresent(q -> a.questionnaireId = q.id);
    assessments.save(a);
    return toDto(a);
  }
  public List<AssessmentResponse> list(UUID orgId) { return assessments.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().map(this::toDto).toList(); }
  public Assessment getEntity(UUID orgId, UUID id) {
    var a = assessments.findById(id).orElseThrow(() -> ApiException.notFound("Assessment not found"));
    if (!a.organizationId.equals(orgId)) throw ApiException.forbidden("Assessment belongs to another organization");
    return a;
  }
  public AssessmentResponse get(UUID orgId, UUID id) { return toDto(getEntity(orgId, id)); }
  public List<ReadinessQuestion> questions() {
    return controls.findAllByOrderByCategoryAscCodeAsc().stream()
      .map(c -> new ReadinessQuestion(c.id, c.category, c.title, c.description, c.weight, c.recommendedEvidence, c.recommendedAction)).toList();
  }
  @Transactional
  public void answer(UUID orgId, Role role, UUID id, AnswerRequest request) {
    RoleGuard.requireContributor(role);
    var a = getEntity(orgId, id);
    if (a.status == AssessmentStatus.COMPLETED) throw ApiException.badRequest("invalid assessment state");
    if (request.controlId() == null || request.answer() == null) throw ApiException.badRequest("controlId and answer are required");
    var c = controls.findById(request.controlId()).orElseThrow(() -> ApiException.notFound("Control not found"));
    var ans = answers.findByAssessmentIdAndControlId(id, c.id).orElseGet(AssessmentAnswer::new);
    ans.assessment = a; ans.controlId = c.id; ans.category = c.category; ans.questionText = c.title; ans.weight = c.weight;
    ans.questionId = request.questionId();
    ans.recommendedEvidence = c.recommendedEvidence; ans.recommendedAction = c.recommendedAction; ans.answer = request.answer();
    ans.comment = request.comment();
    ans.score = switch (request.answer()) {
      case YES -> (double)c.weight;
      case PARTIAL -> c.weight / 2.0;
      case NO, UNKNOWN, NOT_APPLICABLE -> 0.0;
    };
    answers.save(ans);
  }
  @Transactional
  public ScoreResponse complete(UUID orgId, Role role, UUID id) {
    RoleGuard.requireContributor(role);
    var a = getEntity(orgId, id);
    var saved = answers.findByAssessmentId(id);
    if (saved.isEmpty()) throw ApiException.badRequest("Assessment has no answers");
    var score = scoring.score(a, saved);
    a.status = AssessmentStatus.COMPLETED; a.completedAt = Instant.now(); a.overallScore = score.overallScore(); a.riskLevel = score.riskLevel();
    tasks.generateForAssessment(orgId, id, saved);
    return score;
  }
  public ScoreResponse score(UUID orgId, UUID id) { return scoring.score(getEntity(orgId, id), answers.findByAssessmentId(id)); }
  AssessmentResponse toDto(Assessment a) { return new AssessmentResponse(a.id, a.status, a.overallScore, a.riskLevel, a.createdAt, a.completedAt); }
}
