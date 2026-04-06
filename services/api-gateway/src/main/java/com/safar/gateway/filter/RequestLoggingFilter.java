package com.safar.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = System.currentTimeMillis();

        String query = request.getURI().getRawQuery();
        String path = request.getPath().value() + (query != null ? "?" + query : "");
        String userId = request.getHeaders().getFirst("X-User-Id");
        String userAgent = request.getHeaders().getFirst(HttpHeaders.USER_AGENT);

        log.info("→ {} {} userId={} ua={}",
                request.getMethod(), path,
                userId != null ? userId : "-",
                userAgent != null ? truncate(userAgent, 80) : "-");

        return chain.filter(exchange).doFinally(signal -> {
            long duration = System.currentTimeMillis() - start;
            String responseUserId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            log.info("← {} {} {} {}ms userId={}",
                    exchange.getResponse().getStatusCode(),
                    request.getMethod(),
                    request.getPath(),
                    duration,
                    responseUserId != null ? responseUserId : "-");

            if (duration > 5000) {
                log.warn("SLOW {} {} {}ms", request.getMethod(), request.getPath(), duration);
            }
        });
    }

    private static String truncate(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    @Override
    public int getOrder() {
        return -200; // Runs before JwtAuthFilter
    }
}
