package com.nis2ready.assessments;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.assessments.AssessmentDtos.*;

@RestController
@RequestMapping("/api/assessments")
public class AssessmentController {
  private final AssessmentService service;
  public AssessmentController(AssessmentService service) { this.service = service; }
  @PostMapping
  AssessmentResponse create(Authentication auth) {
    var user = CurrentUser.get(auth);
    return service.create(user.organizationId(), user.userId(), user.role());
  }
  @GetMapping
  List<AssessmentResponse> list(Authentication auth) { return service.list(CurrentUser.get(auth).organizationId()); }
  @GetMapping("/questions")
  List<ReadinessQuestion> questions() { return service.questions(); }
  @GetMapping("/{id}")
  AssessmentResponse get(Authentication auth, @PathVariable UUID id) { return service.get(CurrentUser.get(auth).organizationId(), id); }
  @PostMapping("/{id}/answers")
  void answer(Authentication auth, @PathVariable UUID id, @RequestBody AnswerRequest request) {
    var user = CurrentUser.get(auth);
    service.answer(user.organizationId(), user.role(), id, request);
  }
  @PostMapping("/{id}/complete")
  ScoreResponse complete(Authentication auth, @PathVariable UUID id) {
    var user = CurrentUser.get(auth);
    return service.complete(user.organizationId(), user.role(), id);
  }
  @GetMapping("/{id}/score")
  ScoreResponse score(Authentication auth, @PathVariable UUID id) { return service.score(CurrentUser.get(auth).organizationId(), id); }
}
