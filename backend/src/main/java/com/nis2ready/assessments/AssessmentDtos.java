package com.nis2ready.assessments;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AssessmentDtos {
  public record ReadinessQuestion(UUID controlId, String category, String questionText, String description, int weight,
                                  String recommendedEvidence, String recommendedAction) {}
  public record AnswerRequest(UUID controlId, UUID questionId, AnswerOption answer, String comment) {}
  public record AssessmentResponse(UUID id, AssessmentStatus status, Integer overallScore, RiskLevel riskLevel, Instant createdAt, Instant completedAt) {}
  public record ScoreResponse(UUID assessmentId, int overallScore, Map<String, Integer> categoryScores, RiskLevel riskLevel,
                              List<String> weakestCategories, List<String> topPriorityRecommendedActions) {}
}
