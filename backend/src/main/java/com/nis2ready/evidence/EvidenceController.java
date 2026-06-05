package com.nis2ready.evidence;

import com.nis2ready.security.CurrentUser;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.util.UriUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import static com.nis2ready.evidence.EvidenceDtos.*;

@RestController
@RequestMapping("/api/evidence")
public class EvidenceController {
  private final EvidenceService service;
  public EvidenceController(EvidenceService service) { this.service = service; }
  @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  EvidenceResponse upload(Authentication auth, @RequestParam String title, @RequestParam(required = false) String description,
                          @RequestParam(required = false) String category, @RequestParam(required = false) UUID linkedControlId,
                          @RequestParam(required = false) UUID linkedTaskId, @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate validUntil,
                          @RequestParam MultipartFile file) {
    var user = CurrentUser.get(auth);
    return service.upload(user.organizationId(), user.userId(), user.role(), title, description, category, linkedControlId, linkedTaskId, validUntil, file);
  }
  @GetMapping
  List<EvidenceResponse> list(Authentication auth) { return service.list(CurrentUser.get(auth).organizationId()); }
  @GetMapping("/{id}")
  EvidenceResponse get(Authentication auth, @PathVariable UUID id) { return service.get(CurrentUser.get(auth).organizationId(), id); }
  @PutMapping("/{id}/status")
  EvidenceResponse status(Authentication auth, @PathVariable UUID id, @RequestBody EvidenceStatusRequest request) {
    var user = CurrentUser.get(auth);
    return service.status(user.organizationId(), user.role(), id, request.status());
  }
  @GetMapping("/{id}/download")
  ResponseEntity<?> download(Authentication auth, @PathVariable UUID id) {
    var d = service.download(CurrentUser.get(auth).organizationId(), id);
    String filename = d.filename().replaceAll("[\\r\\n\\t\\x00-\\x1F\\x7F\"]", "_");
    String encoded = UriUtils.encode(filename, java.nio.charset.StandardCharsets.UTF_8);
    return ResponseEntity.ok().contentType(MediaType.parseMediaType(d.contentType()))
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"; filename*=UTF-8''" + encoded).body(d.resource());
  }
  @DeleteMapping("/{id}")
  void delete(Authentication auth, @PathVariable UUID id) {
    var user = CurrentUser.get(auth);
    service.delete(user.organizationId(), user.role(), id);
  }
}
