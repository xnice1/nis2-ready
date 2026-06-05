package com.nis2ready;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;
import static org.assertj.core.api.Assertions.assertThat;

class MvpSourceGuardTest {
  private static final Path ROOT = Path.of("").toAbsolutePath();

  @Test
  void seedsAtLeastThirtyControlsAndPolicyDisclaimers() throws Exception {
    String seed = Files.readString(ROOT.resolve("src/main/resources/db/migration/V2__seed_controls_and_templates.sql"));

    long controls = Pattern.compile("'00000000-0000-0000-0000-0000000000\\d{2}'").matcher(seed).results().count();

    assertThat(controls).isGreaterThanOrEqualTo(30);
    assertThat(seed).contains("Template for internal preparation only");
    assertThat(seed).contains("Review with a qualified advisor before official adoption");
  }

  @Test
  void scopingAndReportsKeepLegalDisclaimerLanguage() throws Exception {
    String scoping = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/questionnaires/ScopingService.java"));
    String reports = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/reports/ReportService.java"));

    assertThat(scoping).contains("not legal advice");
    assertThat(reports).contains("not legal advice").contains("not legal advice, certification, or proof of official compliance");
  }

  @Test
  void businessEndpointsRequireAuthenticationByDefault() throws Exception {
    String security = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/config/SecurityConfig.java"));

    assertThat(security).contains("/api/auth/register").contains("/api/auth/login");
    assertThat(security).contains("anyRequest().authenticated()");
    assertThat(security).contains("formLogin").contains("httpBasic").contains("logout");
  }

  @Test
  void scopingAndLinkedEvidenceAreOrganizationScoped() throws Exception {
    String scoping = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/questionnaires/ScopingService.java"));
    String evidence = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/evidence/EvidenceService.java"));
    String task = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/tasks/TaskService.java"));

    assertThat(scoping).contains("resultsByOrganization").doesNotContain("lastResult");
    assertThat(evidence).contains("findByIdAndOrganizationId(taskId, orgId)");
    assertThat(task).contains("Assessment belongs to another organization");
  }

  @Test
  void jwtAndFileUploadHaveExplicitHardening() throws Exception {
    String jwt = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/security/JwtService.java"));
    String files = Files.readString(ROOT.resolve("src/main/java/com/nis2ready/files/FileStorageService.java"));

    assertThat(jwt).contains("JWT_SECRET must be at least 32 bytes").contains("requireIssuer").contains("JWT organization claim is required");
    assertThat(files).contains("maxSizeBytes").contains("validateContent").contains("Files.deleteIfExists");
  }
}
