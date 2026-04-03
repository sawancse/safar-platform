package com.safar.listing.client;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class HostKycClient {

    private final RestClient restClient;

    public HostKycClient(
            @Value("${services.user-service.url:http://localhost:8092}") String userServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getKycStatusFallback")
    @Retry(name = "userService")
    public Map<String, Object> getKycStatus(UUID hostId) {
        return restClient.get()
                .uri("/api/v1/internal/hosts/{hostId}/kyc-status", hostId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    public Map<String, Object> getKycStatusFallback(UUID hostId, Throwable t) {
        log.warn("Could not reach user-service for host {} KYC check: {}", hostId, t.getMessage());
        // Fail open — allow verify if user-service is down (admin can override)
        return Map.of("status", "UNKNOWN", "verified", false);
    }
}
