package com.nis2ready.questionnaires;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "questions")
public class Question {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @ManyToOne(optional = false)
  @JoinColumn(name = "questionnaire_id")
  public Questionnaire questionnaire;
  public String category;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String text;
  @Column(columnDefinition = "TEXT")
  public String description;
  public int weight;
  @Column(columnDefinition = "TEXT")
  public String recommendedEvidence;
  @Column(columnDefinition = "TEXT")
  public String recommendedAction;
  public int sortOrder;
}
