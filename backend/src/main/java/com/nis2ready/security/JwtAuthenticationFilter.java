package com.nis2ready.security;

import com.nis2ready.organizations.MembershipRepository;
import com.nis2ready.users.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserRepository users;
  private final MembershipRepository memberships;
  public JwtAuthenticationFilter(JwtService jwtService, UserRepository users, MembershipRepository memberships) {
    this.jwtService = jwtService;
    this.users = users;
    this.memberships = memberships;
  }
  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header != null && header.startsWith("Bearer ")) {
      try {
        var claims = jwtService.parse(header.substring(7));
        var user = users.findById(claims.userId()).orElse(null);
        if (user != null && user.enabled) {
          memberships.findByUser_IdAndOrganization_Id(user.id, claims.organizationId()).ifPresent(m -> {
            var principal = AppUserPrincipal.of(user, m.organization, m.role);
            var auth = new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + m.role.name())));
            SecurityContextHolder.getContext().setAuthentication(auth);
          });
        }
      } catch (Exception ignored) {
        SecurityContextHolder.clearContext();
      }
    }
    chain.doFilter(request, response);
  }
}
