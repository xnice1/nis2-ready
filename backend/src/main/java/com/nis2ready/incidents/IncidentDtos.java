package com.nis2ready.incidents;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class IncidentDtos {
  public record IncidentRequest(String title, String description, IncidentSeverity severity, IncidentStatus status,
                                Instant detectedAt, Instant reportedInternallyAt, String affectedSystems,
                                UUID ownerUserId, Boolean generateDefaultActions) {}
  public record IncidentActionRequest(String description, IncidentActionStatus status) {}
  public record IncidentDetail(Incident incident, List<IncidentAction> actions) {}
}
