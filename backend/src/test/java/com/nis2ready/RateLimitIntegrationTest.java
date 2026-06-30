package com.nis2ready;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
  "app.rate-limit.api-requests-per-minute=2",
  "app.rate-limit.auth-requests-per-minute=100",
  "app.rate-limit.login-failures-per-window=2",
  "app.rate-limit.login-failure-window-seconds=60"
})
@AutoConfigureMockMvc
class RateLimitIntegrationTest {
  private static final String PASSWORD = "CorrectHorse123!";

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;

  @Test
  void repeatedFailedLoginsAreTemporarilyRateLimitedAndAudited() throws Exception {
    TestAccount account = registerAccount("login-limit");
    String ip = "203.0.113.10";

    failedLogin(account.email(), ip);
    failedLogin(account.email(), ip);

    mvc.perform(post("/api/auth/login")
        .header("X-Forwarded-For", ip)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", account.email(), "password", "wrong-password"))))
      .andExpect(status().isTooManyRequests())
      .andExpect(header().exists("Retry-After"))
      .andExpect(jsonPath("$.message").value("Too many failed login attempts. Try again later."));

    mvc.perform(post("/api/auth/login")
        .header("X-Forwarded-For", ip)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", account.email(), "password", PASSWORD))))
      .andExpect(status().isTooManyRequests());

    mvc.perform(post("/api/auth/login")
        .header("X-Forwarded-For", "203.0.113.11")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", account.email(), "password", PASSWORD))))
      .andExpect(status().isOk());

    mvc.perform(get("/api/audit/events")
        .header(AUTHORIZATION, bearer(account.token())))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[*].eventType", hasItem("LOGIN_RATE_LIMITED")));
  }

  @Test
  void authenticatedApiRequestsAreRateLimitedPerUserAndOrganization() throws Exception {
    TestAccount account = registerAccount("api-limit");

    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(account.token())))
      .andExpect(status().isOk());
    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(account.token())))
      .andExpect(status().isOk());

    mvc.perform(get("/api/auth/me").header(AUTHORIZATION, bearer(account.token())))
      .andExpect(status().isTooManyRequests())
      .andExpect(header().exists("Retry-After"))
      .andExpect(jsonPath("$.message").value("Too many requests. Try again later."));
  }

  private void failedLogin(String email, String ip) throws Exception {
    mvc.perform(post("/api/auth/login")
        .header("X-Forwarded-For", ip)
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of("email", email, "password", "wrong-password"))))
      .andExpect(status().isUnauthorized());
  }

  private TestAccount registerAccount(String prefix) throws Exception {
    String email = prefix + "-" + UUID.randomUUID() + "@example.test";
    MvcResult result = mvc.perform(post("/api/auth/register")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json(Map.of(
          "email", email,
          "password", PASSWORD,
          "firstName", "Rate",
          "lastName", "Limiter",
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
    return new TestAccount(
      email,
      body.path("token").asText()
    );
  }

  private String bearer(String token) {
    return "Bearer " + token;
  }

  private String json(Object value) throws Exception {
    return objectMapper.writeValueAsString(value);
  }

  private record TestAccount(String email, String token) {}
}
