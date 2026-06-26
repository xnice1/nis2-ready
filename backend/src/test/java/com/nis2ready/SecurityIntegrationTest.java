package com.nis2ready;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nis2ready.files.StoredFile;
import com.nis2ready.files.StoredFileRepository;
import com.nis2ready.organizations.Membership;
import com.nis2ready.organizations.MembershipRepository;
import com.nis2ready.organizations.OrganizationRepository;
import com.nis2ready.security.JwtService;
import com.nis2ready.users.Role;
import com.nis2ready.users.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
  "app.evidence-storage-path=./target/security-test-evidence",
  "app.max-evidence-file-size-bytes=32"
})
@AutoConfigureMockMvc
class SecurityIntegrationTest {
  private static final String TEST_SECRET = "change-this-development-secret-change-this";
  private static final String TEST_ISSUER = "nis2-ready-test";
  private static final String PASSWORD = "CorrectHorse123!";
  private static final Path EVIDENCE_ROOT = Path.of("target/security-test-evidence").toAbsolutePath().normalize();

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired UserRepository users;
  @Autowired OrganizationRepository organizations;
  @Autowired MembershipRepository memberships;
  @Autowired StoredFileRepository storedFiles;
  @Autowired PasswordEncoder passwordEncoder;
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
  void registrationHashesPasswordsWithBCryptAndRejectsBadLogin() throws Exception {
    TestAccount account = registerAccount("bcrypt");

    var user = users.findByEmailIgnoreCase(account.email()).orElseThrow();
    assertThat(user.passwordHash).isNotEqualTo(PASSWORD);
    assertThat(user.passwordHash).startsWith("$2a$");
    assertThat(passwordEncoder.matches(PASSWORD, user.passwordHash)).isTrue();

    mvc.perform(post("/api/auth/login")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", account.email(), "password", "wrong-password"))))
      .andExpect(status().isUnauthorized())
      .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("token"))));
  }

  @Test
  void protectedEndpointsRejectMissingMalformedOrglessAndWrongOrganizationTokens() throws Exception {
    TestAccount account = registerAccount("jwt-primary");
    TestAccount other = registerAccount("jwt-other");

    mvc.perform(get("/api/auth/me"))
      .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, "Bearer not-a-token"))
      .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(tokenWithoutOrganizationClaim(account.userId()))))
      .andExpect(status().isUnauthorized());

    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(tokenWithWrongIssuer(account.userId(), account.organizationId()))))
      .andExpect(status().isUnauthorized());

    String wrongOrganizationToken = jwtService.create(account.userId(), other.organizationId());
    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(wrongOrganizationToken)))
      .andExpect(status().isUnauthorized());

    assertThatThrownBy(() -> jwtService.create(account.userId(), null))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("organizationId");
  }

  @Test
  void crossOrganizationUsersCannotReadEvidenceOrAssessments() throws Exception {
    TestAccount tenantA = registerAccount("tenant-a");
    TestAccount tenantB = registerAccount("tenant-b");

    UUID evidenceId = uploadTenantAEvidence(tenantA.token());
    UUID assessmentId = createAssessment(tenantA.token());

    mvc.perform(get("/api/evidence/{id}", evidenceId).header(AUTHORIZATION, bearer(tenantB.token())))
      .andExpect(status().isNotFound());

    mvc.perform(get("/api/evidence/{id}/download", evidenceId).header(AUTHORIZATION, bearer(tenantB.token())))
      .andExpect(status().isNotFound());

    mvc.perform(get("/api/evidence").header(AUTHORIZATION, bearer(tenantB.token())))
      .andExpect(status().isOk())
      .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Tenant A evidence"))));

    mvc.perform(get("/api/assessments/{id}", assessmentId).header(AUTHORIZATION, bearer(tenantB.token())))
      .andExpect(status().isForbidden());
  }

  @Test
  void viewerRoleIsReadOnlyForOrganizationTasksAndEvidence() throws Exception {
    TestAccount owner = registerAccount("role-owner");
    TestAccount viewer = registerAccount("role-viewer");
    String viewerToken = tokenForMembership(viewer.userId(), owner.organizationId(), Role.VIEWER);

    mvc.perform(get("/api/organizations/current").header(AUTHORIZATION, bearer(viewerToken)))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value(owner.organizationId().toString()));

    mvc.perform(put("/api/organizations/current")
        .header(AUTHORIZATION, bearer(viewerToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("name", "Viewer Rename Attempt"))))
      .andExpect(status().isForbidden());

    mvc.perform(post("/api/tasks")
        .header(AUTHORIZATION, bearer(viewerToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of(
          "title", "Viewer task",
          "description", "Viewer should not write",
          "category", "Access control"
        ))))
      .andExpect(status().isForbidden());

    MockMultipartFile file = textFile("viewer.txt", "viewer cannot upload");
    mvc.perform(multipart("/api/evidence/upload")
        .file(file)
        .header(AUTHORIZATION, bearer(viewerToken))
        .param("title", "Viewer evidence"))
      .andExpect(status().isForbidden());
  }

  @Test
  void adminsCannotInviteOwners() throws Exception {
    TestAccount owner = registerAccount("admin-owner");
    TestAccount admin = registerAccount("admin-user");
    TestAccount invited = registerAccount("owner-candidate");
    String adminToken = tokenForMembership(admin.userId(), owner.organizationId(), Role.ADMIN);

    mvc.perform(post("/api/organizations/invite")
        .header(AUTHORIZATION, bearer(adminToken))
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", invited.email(), "role", "OWNER"))))
      .andExpect(status().isForbidden());
  }

  @Test
  void evidenceUploadSanitizesFilenamesStoresGeneratedNamesAndDoesNotExposeStoragePaths() throws Exception {
    TestAccount owner = registerAccount("evidence-owner");
    MockMultipartFile file = textFile("C:\\sensitive\\customer\\secret.txt", "confidential");

    MvcResult result = mvc.perform(multipart("/api/evidence/upload")
        .file(file)
        .header(AUTHORIZATION, bearer(owner.token()))
        .param("title", "Customer evidence")
        .param("category", "Access control"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.originalFilename").value("secret.txt"))
      .andExpect(jsonPath("$.contentType").value("text/plain"))
      .andReturn();

    String response = result.getResponse().getContentAsString();
    assertThat(response).doesNotContain("storagePath", "storedFilename", "security-test-evidence", "sensitive", "customer");

    JsonNode body = objectMapper.readTree(response);
    UUID fileId = UUID.fromString(body.path("fileId").asText());
    StoredFile stored = storedFiles.findById(fileId).orElseThrow();

    assertThat(stored.organizationId).isEqualTo(owner.organizationId());
    assertThat(stored.originalFilename).isEqualTo("secret.txt");
    assertThat(stored.storedFilename).matches("[0-9a-fA-F\\-]{36}\\.txt");
    assertThat(stored.storagePath).isEqualTo(owner.organizationId() + "/" + stored.storedFilename);
    assertThat(EVIDENCE_ROOT.resolve(stored.storagePath).normalize()).startsWith(EVIDENCE_ROOT);
    assertThat(Files.exists(EVIDENCE_ROOT.resolve(stored.storagePath))).isTrue();
  }

  @Test
  void evidenceUploadRejectsUnsupportedExtensionsContentMismatchesAndOversizedFiles() throws Exception {
    TestAccount owner = registerAccount("file-policy");

    mvc.perform(multipart("/api/evidence/upload")
        .file(new MockMultipartFile("file", "malware.exe", "application/octet-stream", "MZ".getBytes(StandardCharsets.UTF_8)))
        .header(AUTHORIZATION, bearer(owner.token()))
        .param("title", "Executable"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("unsupported file type"));

    mvc.perform(multipart("/api/evidence/upload")
        .file(new MockMultipartFile("file", "fake.pdf", "application/pdf", "not a pdf".getBytes(StandardCharsets.UTF_8)))
        .header(AUTHORIZATION, bearer(owner.token()))
        .param("title", "Fake PDF"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("file content does not match supported file type"));

    mvc.perform(multipart("/api/evidence/upload")
        .file(new MockMultipartFile("file", "large.txt", "text/plain", "abcdefghijklmnopqrstuvwxyz0123456789".getBytes(StandardCharsets.UTF_8)))
        .header(AUTHORIZATION, bearer(owner.token()))
        .param("title", "Too large"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("file too large"));
  }

  private TestAccount registerAccount(String prefix) throws Exception {
    String email = prefix + "-" + UUID.randomUUID() + "@example.test";
    MvcResult result = mvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of(
          "email", email,
          "password", PASSWORD,
          "firstName", "Security",
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

  private UUID uploadTenantAEvidence(String token) throws Exception {
    MvcResult result = mvc.perform(multipart("/api/evidence/upload")
        .file(textFile("tenant-a.txt", "tenant secret"))
        .header(AUTHORIZATION, bearer(token))
        .param("title", "Tenant A evidence"))
      .andExpect(status().isOk())
      .andReturn();
    return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
  }

  private UUID createAssessment(String token) throws Exception {
    MvcResult result = mvc.perform(post("/api/assessments").header(AUTHORIZATION, bearer(token)))
      .andExpect(status().isOk())
      .andReturn();
    return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asText());
  }

  private String tokenForMembership(UUID userId, UUID organizationId, Role role) {
    var user = users.findById(userId).orElseThrow();
    var organization = organizations.findById(organizationId).orElseThrow();
    var membership = new Membership();
    membership.user = user;
    membership.organization = organization;
    membership.role = role;
    memberships.saveAndFlush(membership);
    return jwtService.create(user.id, organization.id);
  }

  private String tokenWithoutOrganizationClaim(UUID userId) {
    return Jwts.builder()
      .issuer(TEST_ISSUER)
      .subject(userId.toString())
      .issuedAt(Date.from(Instant.now()))
      .expiration(Date.from(Instant.now().plusSeconds(3600)))
      .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
      .compact();
  }

  private String tokenWithWrongIssuer(UUID userId, UUID organizationId) {
    return Jwts.builder()
      .issuer("wrong-issuer")
      .subject(userId.toString())
      .claim("org", organizationId.toString())
      .issuedAt(Date.from(Instant.now()))
      .expiration(Date.from(Instant.now().plusSeconds(3600)))
      .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
      .compact();
  }

  private MockMultipartFile textFile(String filename, String content) {
    return new MockMultipartFile("file", filename, "text/plain", content.getBytes(StandardCharsets.UTF_8));
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private record TestAccount(String email, String token, UUID userId, UUID organizationId) {}
}
