package com.nis2ready.questionnaires;

import java.util.List;

public class ScopingDtos {
  public enum ScopingOutcome { LIKELY_IN_SCOPE, POSSIBLY_IN_SCOPE, PROBABLY_OUT_OF_DIRECT_SCOPE, NEEDS_EXPERT_REVIEW }
  public record ScopingQuestion(String key, String label, String type, List<String> options) {}
  public record ScopingRequest(String country, String sector, String employeeCount, String turnover, boolean providesItServices,
                               boolean providesManagedServices, boolean providesDigitalInfrastructure,
                               boolean supportsCriticalSector, boolean criticalSupplyChain, boolean clientsAskForCyberDocs) {}
  public record ScopingResult(ScopingOutcome outcome, int confidence, List<String> reasons, String disclaimer) {}
}
