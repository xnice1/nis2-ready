package com.nis2ready.tasks;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.tasks.TaskDtos.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
  private final TaskService service;
  public TaskController(TaskService service) { this.service = service; }
  @GetMapping
  List<RemediationTask> list(Authentication auth) { return service.list(CurrentUser.get(auth).organizationId()); }
  @PostMapping
  RemediationTask create(Authentication auth, @RequestBody TaskRequest request) {
    var user = CurrentUser.get(auth);
    return service.create(user.organizationId(), user.role(), request);
  }
  @GetMapping("/{id}")
  RemediationTask get(Authentication auth, @PathVariable UUID id) { return service.get(CurrentUser.get(auth).organizationId(), id); }
  @PutMapping("/{id}")
  RemediationTask update(Authentication auth, @PathVariable UUID id, @RequestBody TaskRequest request) {
    var user = CurrentUser.get(auth);
    return service.update(user.organizationId(), user.role(), id, request);
  }
  @DeleteMapping("/{id}")
  void delete(Authentication auth, @PathVariable UUID id) {
    var user = CurrentUser.get(auth);
    service.delete(user.organizationId(), user.role(), id);
  }
}
