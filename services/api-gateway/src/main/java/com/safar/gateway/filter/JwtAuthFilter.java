package com.safar.gateway.filter;

import com.safar.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // Paths that never require a token
    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/v1/auth/otp/send",
            "/api/v1/auth/otp/verify",
            "/api/v1/auth/otp/email/send",
            "/api/v1/auth/otp/email/verify",
            "/api/v1/auth/google/signin",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();
        HttpMethod method = request.getMethod();

        if (isPublic(path, method)) {
            return chain.filter(exchange);
        }

        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        String bearer = authHeaders.get(0);
        if (!bearer.startsWith("Bearer ")) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        try {
            Claims claims = jwtUtil.validateToken(bearer.substring(7));
            // Propagate user identity to downstream services via headers
            ServerHttpRequest mutated = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Role", jwtUtil.extractRole(claims))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (JwtException e) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (PUBLIC_PATHS.contains(path)) return true;
        if (path.startsWith("/api/v1/auth/")
                && !path.equals("/api/v1/auth/password/set")
                && !path.equals("/api/v1/auth/password/change")) return true;
        // Public read access for listing search/browse (exclude auth-required paths)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/listings")
                && !path.contains("/mine")
                && !path.contains("/verification-readiness")
                && !path.contains("/investment-signal")) return true;
        // All search endpoints are public (no login required to search)
        if (path.startsWith("/api/v1/search")) return true;
        // Razorpay webhook — signed by Razorpay, no user JWT
        if (path.startsWith("/api/v1/payments/webhook")) return true;
        // Public listing reviews (GET only)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/reviews/listing")) return true;
        // Medical tourism, experiences, aashray — public read access
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/medical-stay")) return true;
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/experiences")) return true;
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/aashray")) return true;
        if (HttpMethod.POST.equals(method) && path.equals("/api/v1/aashray/organizations")) return true;
        // Nomad feed — public read
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/nomad")) return true;
        // User avatars — public read
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/users/avatars")) return true;
        // Public host profiles — public read
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/users/hosts/")) return true;
        return false;
    }

    private Mono<Void> onError(ServerWebExchange exchange, HttpStatus status) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        return response.setComplete();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
