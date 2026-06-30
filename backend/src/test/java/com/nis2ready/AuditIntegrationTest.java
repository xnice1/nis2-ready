package com.nis2ready;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nis2ready.organizations.Membership;
import com.nis2ready.organizations.MembershipRepository;
import com.nis2ready.organizations.OrganizationRepository;
import com.nis2ready.security.JwtService;
import com.nis2ready.users.Role;
import com.nis2ready.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
  "app.evidence-storage-path=./target/audit-test-evidence",
  "app.max-evidence-file-size-bytes=10485760"
})
@AutoConfigureMockMvc
class AuditIntegrationTest {
  private static final String PASSWORD = "CorrectHorse123!";
  private static final Path EVIDENCE_ROOT = Path.of("target/audit-test-evidence").toAbsolutePath().normalize();

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired OrganizationRepository organizations;
  @Autowired MembershipRepository memberships;
  @Autowired JwtService jwtService;

  @BeforeEach
  void cleanEvidenceRoot() throws Exception {
    if (Files.exists(EVIDENCE_ROOT)) {
      try (var paths = Files.walk(EVIDENCE_ROOT)) {
        paths.sorted(Comparator.reverseOrder()).forEach(path -> {
          try {
            Files.deleteIfExists(path);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
      }
    }
  }

  @Test
  void auditEventsAreWrittenForSensitiveActionsWithoutRawSecretsOrStoragePaths() throws Exception {
    TestAccount owner = registerAccount("audit-owner");
    failedLogin(owner.email());
    login(owner.email());

    mvc.perform(put("/api/organizations/current")
        .header(AUTHORIZATION, bearer(owner.token()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("name", "Audit Updated Org"))))
      .andExpect(status().isOk());

    UUID evidenceId = uploadEvidence(owner.token(), "C:\\tenant\\evidence\\board-passwords.txt", "Audit evidence");

    mvc.perform(get("/api/evidence/{id}/download", evidenceId).header(AUTHORIZATION, bearer(owner.token())))
      .andExpect(status().isOk());

    mvc.perform(put("/api/evidence/{id}/status", evidenceId)
        .header(AUTHORIZATION, bearer(owner.token()))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("status", "ACCEPTED"))))
      .andExpect(status().isOk());

    UUID taskId = createAuditTask(owner.token());

    mvc.perform(delete("/api/tasks/{id}", taskId).header(AUTHORIZATION, bearer(owner.token())))
      .andExpect(status().isOk());

    mvc.perform(delete("/api/evidence/{id}", evidenceId).header(AUTHORIZATION, bearer(owner.token())))
      .andExpect(status().isOk());

    mvc.perform(get("/api/audit/events")
        .header(AUTHORIZATION, bearer(owner.token()))
        .param("limit", "50"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[*].eventType", hasItem("USER_REGISTERED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("LOGIN_FAILED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("LOGIN_SUCCEEDED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("ORGANIZATION_UPDATED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("EVIDENCE_UPLOADED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("EVIDENCE_DOWNLOADED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("EVIDENCE_STATUS_CHANGED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("TASK_CREATED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("TASK_DELETED")))
      .andExpect(jsonPath("$[*].eventType", hasItem("EVIDENCE_DELETED")))
      .andExpect(content().string(not(containsString(PASSWORD))))
      .andExpect(content().string(not(containsString(owner.token()))))
      .andExpect(content().string(not(containsString("storagePath"))))
      .andExpect(content().string(not(containsString("storedFilename"))))
      .andExpect(content().string(not(containsString("audit-test-evidence"))))
      .andExpect(content().string(not(containsString("C:\\tenant\\evidence"))));
  }

  @Test
  void auditApiIsOrganizationScopedAndRestrictedToOwnersAndAdmins() throws Exception {
    TestAccount tenantAOwner = registerAccount("tenant-a-audit");
    TestAccount tenantBOwner = registerAccount("tenant-b-audit");
    TestAccount viewer = registerAccount("tenant-a-viewer");
    String viewerToken = tokenForViewerMembership(viewer.userId(), tenantAOwner.organizationId());

    uploadEvidence(tenantAOwner.token(), "tenant-a-audit.txt", "Tenant A confidential audit marker");

    mvc.perform(get("/api/audit/events").header(AUTHORIZATION, bearer(viewerToken)))
      .andExpect(status().isForbidden());

    mvc.perform(get("/api/audit/events").header(AUTHORIZATION, bearer(tenantBOwner.token())))
      .andExpect(status().isOk())
      .andExpect(content().string(not(containsString("Tenant A confidential audit marker"))))
      .andExpect(content().string(not(containsString(tenantAOwner.organizationId().toString()))));
  }

  private TestAccount registerAccount(String prefix) throws Exception {
    String email = prefix + "-" + UUID.randomUUID() + "@example.test";
    MvcResult result = mvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of(
          "email", email,
          "password", PASSWORD,
          "firstName", "Audit",
          "lastName", "Tester",
          "organizationName", prefix + " Org",
          "country", "PL",
          "sector", "Digital services",
          "employeeCountRange", "10-49",
          "annualTurnoverRange", "under-10m",
          "organizationType", "COMPANY"
        ))))
      .andExpect(status().isOk())
      .andReturn();

    JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
    JsonNode user = body.path("user");
    return new TestAccount(
      email,
      body.path("token").asText(),
      UUID.fromString(user.path("id").asText()),
      UUID.fromString(user.path("organizationId").asText())
    );
  }

  private void login(String email) throws Exception {
    mvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", email, "password", PASSWORD))))
      .andExpect(status().isOk());
  }

  private void failedLogin(String email) throws Exception {
    mvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", email, "password", "wrong-password"))))
      .andExpect(status().isUnauthorized());
  }

  private UUID uploadEvidence(String token, String filename, String title) throws Exception {
    var file = new MockMultipartFile("file", filename, "text/plain", "audit evidence".getBytes(StandardCharsets.UTF_8));
    MvcResult result = mvc.perform(multipart("/api/evidence/upload")
        .file(file)
        .header(AUTHORIZATION, bearer(token))
        .param("title", title))
      .andExpect(status().isOk())
      .andReturn();
    return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
  }

  private UUID createAuditTask(String token) throws Exception {
    MvcResult result = mvc.perform(post("/api/tasks")
        .header(AUTHORIZATION, bearer(token))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of(
          "title", "Audit task",
          "description", "Task created to verify audit event writing",
          "category", "Audit"
        ))))
      .andExpect(status().isOk())
      .andReturn();
    return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
  }

  private String tokenForViewerMembership(UUID userId, UUID organizationId) {
    var user = users.findById(userId).orElseThrow();
    var organization = organizations.findById(organizationId).orElseThrow();
    var membership = new Membership();
    membership.user = user;
    membership.organization = organization;
    membership.role = Role.VIEWER;
    memberships.saveAndFlush(membership);
    return jwtService.create(user.id, organization.id);
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private record TestAccount(String email, String token, UUID userId, UUID organizationId) {}
}
