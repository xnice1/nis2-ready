package com.nis2ready.questionnaires;

import com.nis2ready.security.CurrentUser;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import static com.nis2ready.questionnaires.ScopingDtos.*;

@RestController
@RequestMapping("/api/scoping")
public class ScopingController {
  private final ScopingService service;
  public ScopingController(ScopingService service) { this.service = service; }
  @GetMapping("/questions")
  List<ScopingQuestion> questions() { return service.questions(); }
  @PostMapping("/assess")
  ScopingResult assess(Authentication auth, @RequestBody ScopingRequest request) { return service.assess(CurrentUser.get(auth).organizationId(), request); }
  @GetMapping("/result")
  ScopingResult result(Authentication auth) { return service.last(CurrentUser.get(auth).organizationId()); }
}
