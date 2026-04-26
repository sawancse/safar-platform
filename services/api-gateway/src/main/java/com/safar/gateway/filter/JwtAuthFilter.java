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
        log.info("AUTH HEADER present={}, value={}", authHeaders != null && !authHeaders.isEmpty(),
                authHeaders != null && !authHeaders.isEmpty() ? authHeaders.get(0).substring(0, Math.min(30, authHeaders.get(0).length())) + "..." : "NONE");
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
                    log.warn("JWT validation failed for {} {}: {}", method, path, e.getMessage());
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
                && !path.equals("/api/v1/auth/password/change")
                && !path.equals("/api/v1/auth/pin/set")
                && !path.equals("/api/v1/auth/pin/change")
                && !path.equals("/api/v1/auth/pin/reset")
                && !(path.equals("/api/v1/auth/pin") && HttpMethod.DELETE.equals(method))) return true;
        // Public read access for listing search/browse (exclude auth-required paths)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/listings")
                && !path.contains("/mine")
                && !path.contains("/verification-readiness")
                && !path.contains("/investment-signal")) return true;
        // All search endpoints are public (no login required to search)
        if (path.startsWith("/api/v1/search")) return true;
        // Agreement view/download — requires auth (contains PII: name, Aadhaar, address, rent)
        // Removed public access to prevent unauthorized agreement viewing
        // Razorpay webhook — signed by Razorpay, no user JWT
        if (path.startsWith("/api/v1/payments/webhook")) return true;
        if (path.startsWith("/api/v1/payments/tenancy/webhook")) return true;
        // Flight provider webhooks (Duffel etc.) — HMAC-verified inside flight-service, no user JWT
        if (path.startsWith("/api/v1/flights/webhooks/")) return true;
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
        // Locations autocomplete (city → localities) — public, used by sign-up & cook registration
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/locations")) return true;
        // Safar Cooks — public browse and chef profiles (exclude admin and /me which needs auth)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/chefs")
                && !path.contains("/admin") && !path.contains("/me")) return true;
        // Chef event pricing — public read (exclude admin and /me)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/chef-events/pricing")
                && !path.contains("/admin") && !path.contains("/me")) return true;
        // Aggregate ratings for the services landing — public
        if (HttpMethod.GET.equals(method) && path.equals("/api/v1/chef-events/aggregate-ratings")) return true;
        // Chef events — public browse and event inquiry (exclude /my and /chef which need auth)
        if (path.startsWith("/api/v1/chef-events") && !path.contains("/my") && !path.contains("/chef")
                && !path.contains("/admin")) return true;
        // Chef bookings — public read for single booking detail, tracking & invoices
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/chef-bookings/")
                && !path.contains("/my") && !path.contains("/chef")) return true;
        // Experience reviews — public read
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/reviews/experience")) return true;
        // Broker profiles — public search and view (exclude /me and /admin)
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/brokers")
                && !path.contains("/me") && !path.contains("/admin")) return true;
        // VAS: Agreement templates & stamp duty calculator — public
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/agreements/templates")) return true;
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/agreements/stamp-duty")) return true;
        // VAS: Home loan — banks, EMI calculator, eligibility are public reads
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/homeloan/banks")) return true;
        if (HttpMethod.POST.equals(method) && path.equals("/api/v1/homeloan/emi/calculate")) return true;
        // VAS: Legal packages & advocates — public browse
        if (HttpMethod.GET.equals(method) && path.equals("/api/v1/legal/packages")) return true;
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/legal/advocates")) return true;
        // VAS: Interior designers & material catalog — public browse
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/interiors/designers")) return true;
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/interiors/materials/catalog")) return true;
        // Lead capture — public (no auth needed for email capture, activity tracking, alerts, calculator)
        if (path.startsWith("/api/v1/leads")) return true;
        // Dish catalog — public browse and cook matching
        if (HttpMethod.GET.equals(method) && path.startsWith("/api/v1/dishes")) return true;
        if (HttpMethod.POST.equals(method) && path.equals("/api/v1/dishes/match-chefs")) return true;
        // Flights — search is public (no login needed to search flights)
        if (HttpMethod.GET.equals(method) && path.equals("/api/v1/flights/search")) return true;
        // Internal service-to-service endpoints — no user auth
        if (path.startsWith("/api/v1/internal/")) return true;
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
