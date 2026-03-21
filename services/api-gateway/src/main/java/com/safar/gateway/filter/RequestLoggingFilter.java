package com.safar.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
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

        log.info("→ {} {}", request.getMethod(), request.getPath());

        return chain.filter(exchange).doFinally(signal ->
                log.info("← {} {} {}ms",
                        exchange.getResponse().getStatusCode(),
                        request.getPath(),
                        System.currentTimeMillis() - start)
        );
    }

    @Override
    public int getOrder() {
        return -200; // Runs before JwtAuthFilter
    }
}
