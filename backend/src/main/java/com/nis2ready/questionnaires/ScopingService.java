package com.nis2ready.questionnaires;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import static com.nis2ready.questionnaires.ScopingDtos.*;

@Service
public class ScopingService {
  private final Map<UUID, ScopingResult> resultsByOrganization = new ConcurrentHashMap<>();
  public List<ScopingQuestion> questions() {
    return List.of(
      new ScopingQuestion("country", "Country", "text", List.of()),
      new ScopingQuestion("sector", "Sector", "text", List.of()),
      new ScopingQuestion("employeeCount", "Employee count", "select", List.of("1-9", "10-49", "50-249", "250+")),
      new ScopingQuestion("turnover", "Annual turnover", "select", List.of("<10M EUR", "10-50M EUR", "50M+ EUR")),
      new ScopingQuestion("providesItServices", "Provides IT services", "boolean", List.of()),
      new ScopingQuestion("providesManagedServices", "Provides managed services", "boolean", List.of()),
      new ScopingQuestion("providesDigitalInfrastructure", "Provides digital infrastructure services", "boolean", List.of()),
      new ScopingQuestion("supportsCriticalSector", "Supports healthcare, transport, energy, finance, public administration, manufacturing, logistics, water, waste, space, postal/courier, or digital services", "boolean", List.of()),
      new ScopingQuestion("criticalSupplyChain", "Part of a critical supply chain", "boolean", List.of()),
      new ScopingQuestion("clientsAskForCyberDocs", "Clients ask for cybersecurity documentation", "boolean", List.of())
    );
  }
  public ScopingResult assess(UUID orgId, ScopingRequest request) {
    int score = 0;
    var reasons = new ArrayList<String>();
    if ("250+".equals(request.employeeCount()) || "50M+ EUR".equals(request.turnover())) { score += 25; reasons.add("Company size may trigger closer NIS2-style review."); }
    if (request.providesManagedServices()) { score += 30; reasons.add("Managed services are a strong NIS2 relevance signal."); }
    if (request.providesDigitalInfrastructure()) { score += 30; reasons.add("Digital infrastructure services are a strong relevance signal."); }
    if (request.providesItServices()) { score += 15; reasons.add("IT service delivery increases cybersecurity readiness expectations."); }
    if (request.supportsCriticalSector()) { score += 20; reasons.add("Support for critical sectors increases indirect and direct readiness expectations."); }
    if (request.criticalSupplyChain()) { score += 15; reasons.add("Critical supply chain role may create customer and regulatory evidence needs."); }
    if (request.clientsAskForCyberDocs()) { score += 10; reasons.add("Customer documentation requests indicate practical readiness obligations."); }
    ScopingOutcome outcome = score >= 70 ? ScopingOutcome.LIKELY_IN_SCOPE : score >= 35 ? ScopingOutcome.POSSIBLY_IN_SCOPE : score >= 15 ? ScopingOutcome.NEEDS_EXPERT_REVIEW : ScopingOutcome.PROBABLY_OUT_OF_DIRECT_SCOPE;
    if (reasons.isEmpty()) reasons.add("No strong direct scoping indicators were selected.");
    var result = new ScopingResult(outcome, Math.min(95, 45 + score / 2), reasons, "This is an informational estimate only and is not legal advice. Confirm your status with a qualified legal or cybersecurity advisor.");
    resultsByOrganization.put(orgId, result);
    return result;
  }
  public ScopingResult last(UUID orgId) {
    return resultsByOrganization.getOrDefault(orgId, new ScopingResult(ScopingOutcome.NEEDS_EXPERT_REVIEW, 0, List.of("No scoping questionnaire has been completed in this session."), "This is an informational estimate only and is not legal advice. Confirm your status with a qualified legal or cybersecurity advisor."));
  }
}
