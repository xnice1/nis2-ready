package com.nis2ready.reports;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;
import static com.nis2ready.reports.ReportDtos.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
  private final ReportService service;
  public ReportController(ReportService service) { this.service = service; }
  @GetMapping("/readiness")
  ReadinessReport readiness(Authentication auth) { return service.readiness(CurrentUser.get(auth).organizationId()); }
  @GetMapping("/monthly")
  MonthlyProgressReport monthly(Authentication auth) { return service.monthly(CurrentUser.get(auth).organizationId()); }
  @GetMapping("/assessment/{id}")
  ReadinessReport assessment(Authentication auth, @PathVariable UUID id) { return service.assessment(CurrentUser.get(auth).organizationId(), id); }
  @GetMapping("/dashboard")
  Dashboard dashboard(Authentication auth) { return service.dashboard(CurrentUser.get(auth).organizationId()); }
}
