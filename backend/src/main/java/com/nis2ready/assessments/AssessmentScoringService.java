package com.nis2ready.assessments;

import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.nis2ready.assessments.AssessmentDtos.*;

@Service
public class AssessmentScoringService {
  public ScoreResponse score(Assessment assessment, List<AssessmentAnswer> answers) {
    double earned = 0;
    int possible = 0;
    Map<String, List<AssessmentAnswer>> byCategory = answers.stream().collect(Collectors.groupingBy(a -> a.category));
    Map<String, Integer> categoryScores = new LinkedHashMap<>();
    for (var entry : byCategory.entrySet().stream().sorted(Map.Entry.comparingByKey()).toList()) {
      double catEarned = 0;
      int catPossible = 0;
      for (var answer : entry.getValue()) {
        if (answer.answer == AnswerOption.NOT_APPLICABLE) continue;
        catPossible += answer.weight;
        possible += answer.weight;
        double points = switch (answer.answer) {
          case YES -> answer.weight;
          case PARTIAL -> answer.weight / 2.0;
          case NO, UNKNOWN -> 0;
          case NOT_APPLICABLE -> 0;
        };
        catEarned += points;
        earned += points;
      }
      categoryScores.put(entry.getKey(), catPossible == 0 ? 100 : (int)Math.round(catEarned * 100 / catPossible));
    }
    int overall = possible == 0 ? 0 : (int)Math.round(earned * 100 / possible);
    var risk = risk(overall);
    var weakest = categoryScores.entrySet().stream().sorted(Map.Entry.comparingByValue()).limit(4).map(Map.Entry::getKey).toList();
    var actions = answers.stream()
      .filter(a -> a.answer == AnswerOption.NO || a.answer == AnswerOption.UNKNOWN || a.answer == AnswerOption.PARTIAL)
      .sorted(Comparator.comparingInt((AssessmentAnswer a) -> a.weight).reversed())
      .limit(8).map(a -> a.recommendedAction).distinct().toList();
    return new ScoreResponse(assessment.id, overall, categoryScores, risk, weakest, actions);
  }
  public RiskLevel risk(int score) {
    if (score >= 80) return RiskLevel.LOW;
    if (score >= 55) return RiskLevel.MEDIUM;
    if (score >= 30) return RiskLevel.HIGH;
    return RiskLevel.CRITICAL;
  }
}
