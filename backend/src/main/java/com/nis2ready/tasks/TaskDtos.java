package com.nis2ready.tasks;

import java.time.LocalDate;
import java.util.UUID;

public class TaskDtos {
  public record TaskRequest(String title, String description, String category, TaskPriority priority, TaskStatus status,
                            UUID ownerUserId, LocalDate dueDate, UUID relatedControlId, UUID relatedAssessmentId) {}
}
