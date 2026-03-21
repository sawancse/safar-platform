package com.safar.gateway.filter;

import com.safar.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtAuthFilterTest {

    @Mock JwtUtil jwtUtil;
    @InjectMocks JwtAuthFilter filter;

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String VALID_TOKEN = "valid.jwt.token";

    @BeforeEach
    void setUp() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(USER_ID.toString());
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(claims);
        when(jwtUtil.extractRole(claims)).thenReturn("GUEST");
    }

    @Test
    void publicAuthPath_noToken_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/otp/send").build());

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> { chainCalled.set(true); return Mono.empty(); };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void publicGetListing_noToken_passesThrough() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/listings?city=Mumbai").build());

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> { chainCalled.set(true); return Mono.empty(); };

        filter.filter(exchange, chain).block();

        assertThat(chainCalled.get()).isTrue();
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void protectedPath_noToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me/taste-profile").build());

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_validToken_forwardsWithUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me/taste-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + VALID_TOKEN)
                        .build());

        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        GatewayFilterChain chain = ex -> { captured.set(ex); return Mono.empty(); };

        filter.filter(exchange, chain).block();

        assertThat(captured.get()).isNotNull();
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Id"))
                .isEqualTo(USER_ID.toString());
        assertThat(captured.get().getRequest().getHeaders().getFirst("X-User-Role"))
                .isEqualTo("GUEST");
    }

    @Test
    void protectedPath_invalidToken_returns401() {
        when(jwtUtil.validateToken("bad.token")).thenThrow(new JwtException("invalid"));

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me/taste-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token")
                        .build());

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void protectedPath_malformedHeader_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/users/me/taste-profile")
                        .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
                        .build());

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void postListing_noToken_returns401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/listings").build());

        filter.filter(exchange, ex -> Mono.empty()).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
