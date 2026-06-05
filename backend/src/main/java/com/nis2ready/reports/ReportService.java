package com.nis2ready.reports;

import com.nis2ready.assessments.*;
import com.nis2ready.evidence.*;
import com.nis2ready.organizations.OrganizationService;
import com.nis2ready.tasks.*;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import static com.nis2ready.reports.ReportDtos.*;

@Service
public class ReportService {
  private final OrganizationService organizations;
  private final AssessmentRepository assessments;
  private final AssessmentAnswerRepository answers;
  private final AssessmentScoringService scoring;
  private final RemediationTaskRepository tasks;
  private final EvidenceRepository evidence;
  public ReportService(OrganizationService organizations, AssessmentRepository assessments, AssessmentAnswerRepository answers, AssessmentScoringService scoring, RemediationTaskRepository tasks, EvidenceRepository evidence) {
    this.organizations = organizations; this.assessments = assessments; this.answers = answers; this.scoring = scoring; this.tasks = tasks; this.evidence = evidence;
  }
  public ReadinessReport readiness(UUID orgId) {
    var latest = assessments.findFirstByOrganizationIdAndStatusOrderByCompletedAtDesc(orgId, AssessmentStatus.COMPLETED).orElse(null);
    return buildReadinessReport(orgId, latest);
  }
  public MonthlyProgressReport monthly(UUID orgId) {
    var list = assessments.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().filter(a -> a.status == AssessmentStatus.COMPLETED).toList();
    Integer current = list.isEmpty() ? null : list.get(0).overallScore;
    Integer previous = list.size() < 2 ? null : list.get(1).overallScore;
    var high = tasks.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().filter(t -> (t.priority == TaskPriority.HIGH || t.priority == TaskPriority.CRITICAL) && t.status != TaskStatus.DONE).toList();
    return new MonthlyProgressReport(current, previous,
      tasks.findByOrganizationIdAndStatusAndUpdatedAtAfter(orgId, TaskStatus.DONE, Instant.now().minusSeconds(30L * 24 * 60 * 60)),
      high, evidence.findByOrganizationIdAndValidUntilBefore(orgId, LocalDate.now()), Map.of(),
      List.of("Close critical and high priority remediation tasks.", "Review expired evidence.", "Re-run the readiness assessment after major improvements."),
      Instant.now());
  }
  public Dashboard dashboard(UUID orgId) {
    var report = readiness(orgId);
    return new Dashboard(report.latestAssessmentScore(), report.riskLevel(),
      tasks.countByOrganizationIdAndPriorityInAndStatusNot(orgId, List.of(TaskPriority.CRITICAL), TaskStatus.DONE),
      report.evidenceCompleteness(), report.weakestCategories(),
      assessments.findFirstByOrganizationIdAndStatusOrderByCompletedAtDesc(orgId, AssessmentStatus.COMPLETED).map(a -> a.completedAt).orElse(null),
      tasks.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream().limit(6).toList(),
      evidence.countByOrganizationIdAndStatus(orgId, EvidenceStatus.ACCEPTED),
      evidence.countByOrganizationIdAndStatus(orgId, EvidenceStatus.PENDING_REVIEW),
      report.categoryScores());
  }
  public ReadinessReport assessment(UUID orgId, UUID assessmentId) {
    var assessment = assessments.findById(assessmentId).orElseThrow(() -> com.nis2ready.common.ApiException.notFound("Assessment not found"));
    if (!assessment.organizationId.equals(orgId)) throw com.nis2ready.common.ApiException.forbidden("Assessment belongs to another organization");
    return buildReadinessReport(orgId, assessment);
  }
  private ReadinessReport buildReadinessReport(UUID orgId, Assessment assessment) {
    var score = assessment == null ? null : scoring.score(assessment, answers.findByAssessmentId(assessment.id));
    var high = tasks.findByOrganizationIdOrderByCreatedAtDesc(orgId).stream()
      .filter(t -> (t.priority == TaskPriority.HIGH || t.priority == TaskPriority.CRITICAL) && t.status != TaskStatus.DONE).toList();
    return new ReadinessReport(organizations.current(orgId), assessment == null ? null : score.overallScore(),
      score == null ? Map.of() : score.categoryScores(), score == null ? null : score.riskLevel(),
      score == null ? List.of() : score.weakestCategories(), high, evidenceCompleteness(orgId),
      score == null ? List.of("Complete a readiness assessment to generate prioritized actions.") : score.topPriorityRecommendedActions(),
      Instant.now(), "Readiness reports are informational only and are not legal advice, certification, or proof of official compliance. Items marked needs expert review should be validated by qualified advisors.");
  }
  private double evidenceCompleteness(UUID orgId) {
    long total = evidence.countByOrganizationId(orgId);
    if (total == 0) return 0;
    return Math.round((evidence.countByOrganizationIdAndStatus(orgId, EvidenceStatus.ACCEPTED) * 10000.0 / total)) / 100.0;
  }
}
