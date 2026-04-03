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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
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

        // Allow CORS preflight requests through without auth
        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(exchange);
        }

        boolean publicPath = isPublic(path, method);
        log.info("AUTH CHECK: {} {} → public={}", method, path, publicPath);

        // Attempt JWT processing — propagate user identity even on public paths
        List<String> authHeaders = request.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authHeaders != null && !authHeaders.isEmpty()) {
            String bearer = authHeaders.get(0);
            if (bearer.startsWith("Bearer ")) {
                try {
                    Claims claims = jwtUtil.validateToken(bearer.substring(7));
                    ServerHttpRequest mutated = request.mutate()
                            .header("X-User-Id", claims.getSubject())
                            .header("X-User-Role", jwtUtil.extractRole(claims))
                            .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                } catch (JwtException e) {
                    if (!publicPath) {
                        return onError(exchange, HttpStatus.UNAUTHORIZED);
                    }
                    // Public path with invalid token — proceed without auth headers
                }
            } else if (!publicPath) {
                return onError(exchange, HttpStatus.UNAUTHORIZED);
            }
        } else if (!publicPath) {
            return onError(exchange, HttpStatus.UNAUTHORIZED);
        }

        return chain.filter(exchange);
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
        // Agreement view/download — public so tenant can share with family
        if (HttpMethod.GET.equals(method) && path.matches(".*/agreement/(view|pdf|pdf/inline|text)$")) return true;
        // Razorpay webhook — signed by Razorpay, no user JWT
        if (path.startsWith("/api/v1/payments/webhook")) return true;
        if (path.startsWith("/api/v1/payments/tenancy/webhook")) return true;
        // Donations — create and verify are public (anonymous donations allowed), stats are public GET
        if (path.equals("/api/v1/donations") && HttpMethod.POST.equals(method)) return true;
        if (path.equals("/api/v1/donations/verify") && HttpMethod.POST.equals(method)) return true;
        if (path.equals("/api/v1/donations/stats") && HttpMethod.GET.equals(method)) return true;
        if (path.equals("/api/v1/donations/leaderboard") && HttpMethod.GET.equals(method)) return true;
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
        // Sale properties — public read (browse, detail, similar)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/sale-properties")
                && !path.contains("/seller") && !path.contains("/admin")) return true;
        // Locality price trends — public
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/locality-trends")) return true;
        // Builder projects — public read (browse, detail, unit types, construction updates)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/builder-projects")
                && !path.contains("/my-projects") && !path.contains("/admin")) return true;
        // Localities — public read + bulk-import (seed script, no user auth)
        if (path.startsWith("/api/v1/localities")) return true;
        // Safar Cooks — public browse and chef profiles (exclude admin)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/chefs")
                && !path.contains("/admin")) return true;
        // Chef event pricing — public read (exclude admin and /me)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/chef-events/pricing")
                && !path.contains("/admin") && !path.contains("/me")) return true;
        // Chef events — public browse only (creating requires auth)
        if (HttpMethod.GET.equals(method) && (path.equals("/api/v1/chef-events") || path.startsWith("/api/v1/chef-events/"))
                && !path.contains("/my") && !path.contains("/chef")) return true;
        // Chef bookings — public read for single booking detail, tracking & invoices
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/chef-bookings/")
                && !path.contains("/my") && !path.contains("/chef")) return true;
        // Experience reviews — public read
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/reviews/experience")) return true;
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
