package com.nis2ready.questionnaires;

import com.nis2ready.common.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "questionnaires")
public class Questionnaire extends AuditedEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  public QuestionnaireType type;
  @Column(nullable = false)
  public String title;
  @Column(columnDefinition = "TEXT")
  public String description;
  @Column(nullable = false)
  public int version = 1;
  @Column(nullable = false)
  public boolean active = true;
}
