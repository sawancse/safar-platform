package com.safar.user.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Stateless JWT authentication filter.
 *
 * <p>Reads the {@code Authorization: Bearer <token>} header, validates the
 * token via {@link JwtUtil}, and populates the {@link SecurityContextHolder}
 * with the authenticated principal (userId string) and the user's role as a
 * granted authority.</p>
 *
 * <p>If no token is present the request passes through unauthenticated,
 * allowing Spring Security's authorisation rules to reject it if required.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        String uri = request.getRequestURI();
        if (!uri.startsWith("/api/v1/internal")) {
            log.info(">> {} {} | Authorization header: {}", request.getMethod(), uri,
                    header == null ? "MISSING" : header.substring(0, Math.min(header.length(), 30)) + "...");
        }

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                Claims claims = jwtUtil.validateToken(token);
                String userId = claims.getSubject();
                String role = jwtUtil.extractRole(claims);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
                log.info("JWT auth set for userId={} role={}", userId, role);
            } catch (JwtException e) {
                log.warn("JWT validation failed: {} — falling back to gateway headers", e.getMessage());
            } catch (Exception e) {
                log.warn("JWT parsing error: {} — falling back to gateway headers", e.getMessage());
            }
        }

        log.info("Auth after JWT phase: {}", SecurityContextHolder.getContext().getAuthentication());

        // Fallback: trust gateway-propagated headers if JWT validation failed
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String gatewayUserId = request.getHeader("X-User-Id");
            String gatewayRole = request.getHeader("X-User-Role");
            if (gatewayUserId != null && !gatewayUserId.isBlank()) {
                String role = (gatewayRole != null && !gatewayRole.isBlank()) ? gatewayRole : "USER";
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                gatewayUserId, null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
