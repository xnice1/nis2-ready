package com.nis2ready.reports;

import com.nis2ready.assessments.RiskLevel;
import com.nis2ready.evidence.Evidence;
import com.nis2ready.organizations.OrganizationDtos.OrganizationResponse;
import com.nis2ready.tasks.RemediationTask;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportDtos {
  public record ReadinessReport(OrganizationResponse organization, Integer latestAssessmentScore, Map<String, Integer> categoryScores,
                                RiskLevel riskLevel, List<String> weakestCategories, List<RemediationTask> openHighPriorityTasks,
                                double evidenceCompleteness, List<String> recommendedNextActions, Instant generatedAt, String disclaimer) {}
  public record MonthlyProgressReport(Integer currentReadinessScore, Integer previousScore, List<RemediationTask> completedTasksLast30Days,
                                      List<RemediationTask> openCriticalHighTasks, List<Evidence> expiredEvidence,
                                      Map<String, Integer> categoryChanges, List<String> recommendedNextSteps, Instant generatedAt) {}
  public record Dashboard(Integer overallReadinessScore, RiskLevel riskLevel, long openCriticalTasks, double evidenceCompleteness,
                          List<String> weakestCategories, Instant latestAssessmentDate, List<RemediationTask> recentTasks,
                          long acceptedEvidence, long pendingEvidence, Map<String, Integer> categoryScores) {}
}
