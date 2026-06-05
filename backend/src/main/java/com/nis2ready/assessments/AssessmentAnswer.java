package com.nis2ready.assessments;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "assessment_answers", uniqueConstraints = @UniqueConstraint(columnNames = {"assessment_id", "control_id"}))
public class AssessmentAnswer {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @ManyToOne(optional = false)
  @JoinColumn(name = "assessment_id")
  public Assessment assessment;
  @Column(nullable = false)
  public UUID controlId;
  public UUID questionId;
  @Column(nullable = false)
  public String category;
  @Column(nullable = false)
  public String questionText;
  @Column(nullable = false)
  public int weight;
  @Column(columnDefinition = "TEXT")
  public String recommendedEvidence;
  @Column(columnDefinition = "TEXT")
  public String recommendedAction;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public AnswerOption answer;
  @Column(columnDefinition = "TEXT")
  public String comment;
  public Double score;
}
