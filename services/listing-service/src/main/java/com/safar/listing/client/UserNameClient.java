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
public class UserNameClient {

    private final RestClient restClient;

    public UserNameClient(
            @Value("${services.user-service.url}") String userServiceUrl) {
        this.restClient = RestClient.builder().baseUrl(userServiceUrl).build();
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserNameFallback")
    @Retry(name = "userService")
    public String getUserName(UUID userId) {
        Map<String, String> result = restClient.get()
                .uri("/api/v1/internal/users/{userId}/email", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        if (result != null && result.get("name") != null && !result.get("name").isBlank()) {
            return result.get("name");
        }
        return "Host";
    }

    public String getUserNameFallback(UUID userId, Throwable t) {
        log.warn("Could not fetch name for user {}: {}", userId, t.getMessage());
        return "Host";
    }
}
