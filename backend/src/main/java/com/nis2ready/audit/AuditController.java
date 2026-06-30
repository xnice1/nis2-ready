package com.nis2ready.audit;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.nis2ready.audit.AuditDtos.*;

@RestController
@RequestMapping("/api/audit")
public class AuditController {
  private final AuditService service;

  public AuditController(AuditService service) {
    this.service = service;
  }

  @GetMapping("/events")
  List<AuditEventResponse> events(Authentication auth, @RequestParam(defaultValue = "100") int limit) {
    var user = CurrentUser.get(auth);
    return service.list(user.organizationId(), user.role(), limit);
  }
}
