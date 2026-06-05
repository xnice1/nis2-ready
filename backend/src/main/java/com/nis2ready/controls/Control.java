package com.nis2ready.controls;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "controls")
public class Control {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  public UUID id;
  @Column(nullable = false, unique = true)
  public String code;
  @Column(nullable = false)
  public String category;
  @Column(nullable = false)
  public String title;
  @Column(nullable = false, columnDefinition = "TEXT")
  public String description;
  @Column(name = "nis2_reference", columnDefinition = "TEXT")
  public String nis2Reference;
  @Column(columnDefinition = "TEXT")
  public String recommendedEvidence;
  @Column(columnDefinition = "TEXT")
  public String recommendedAction;
  @Column(nullable = false)
  public int weight;
}
