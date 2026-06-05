package com.nis2ready.incidents;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.incidents.IncidentDtos.*;

@RestController
@RequestMapping("/api/incidents")
public class IncidentController {
  private final IncidentService service;
  public IncidentController(IncidentService service) { this.service = service; }
  @GetMapping
  List<Incident> list(Authentication auth) { return service.list(CurrentUser.get(auth).organizationId()); }
  @PostMapping
  IncidentDetail create(Authentication auth, @RequestBody IncidentRequest request) { var u = CurrentUser.get(auth); return service.create(u.organizationId(), u.userId(), u.role(), request); }
  @GetMapping("/{id}")
  IncidentDetail get(Authentication auth, @PathVariable UUID id) { return service.get(CurrentUser.get(auth).organizationId(), id); }
  @PutMapping("/{id}")
  IncidentDetail update(Authentication auth, @PathVariable UUID id, @RequestBody IncidentRequest request) {
    var u = CurrentUser.get(auth);
    return service.update(u.organizationId(), u.role(), id, request);
  }
  @PostMapping("/{id}/actions")
  IncidentAction addAction(Authentication auth, @PathVariable UUID id, @RequestBody IncidentActionRequest request) { var u = CurrentUser.get(auth); return service.addAction(u.organizationId(), id, u.userId(), u.role(), request); }
  @PutMapping("/{id}/actions/{actionId}")
  IncidentAction updateAction(Authentication auth, @PathVariable UUID id, @PathVariable UUID actionId, @RequestBody IncidentActionRequest request) {
    var u = CurrentUser.get(auth);
    return service.updateAction(u.organizationId(), u.role(), id, actionId, request);
  }
}
