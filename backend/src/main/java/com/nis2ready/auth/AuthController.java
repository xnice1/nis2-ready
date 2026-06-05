package com.nis2ready.auth;

import com.nis2ready.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static com.nis2ready.auth.AuthDtos.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService service;
  public AuthController(AuthService service) { this.service = service; }
  @PostMapping("/register")
  AuthResponse register(@Valid @RequestBody RegisterRequest request) { return service.register(request); }
  @PostMapping("/login")
  AuthResponse login(@Valid @RequestBody LoginRequest request) { return service.login(request); }
  @GetMapping("/me")
  MeResponse me(Authentication auth) {
    var user = CurrentUser.get(auth);
    return service.me(user.userId(), user.organizationId());
  }
}
