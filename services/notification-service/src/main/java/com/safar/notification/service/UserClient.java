package com.safar.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
public class UserClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userServiceUrl;

    public UserClient(RestTemplate restTemplate,
                      ObjectMapper objectMapper,
                      @Value("${services.user-service.url}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.userServiceUrl = userServiceUrl;
    }

    @CircuitBreaker(name = "userService", fallbackMethod = "getUserFallback")
    @Retry(name = "userService")
    public UserInfo getUser(String userId) {
        String url = userServiceUrl + "/api/v1/internal/users/" + userId + "/email";
        String json = restTemplate.getForObject(url, String.class);
        try {
            JsonNode node = objectMapper.readTree(json);
            return new UserInfo(
                    node.path("email").asText(""),
                    node.path("name").asText(""),
                    node.path("phone").asText("")
            );
        } catch (Exception e) {
            log.warn("Failed to parse user {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public UserInfo getUserFallback(String userId, Throwable t) {
        log.warn("Failed to fetch user {}: {}", userId, t.getMessage());
        return null;
    }

    /**
     * Fetch the user's registered Expo push tokens for push delivery.
     * Returns empty list on any failure (push silently no-ops).
     */
    public List<String> getPushTokens(UUID userId) {
        if (userId == null) return List.of();
        try {
            String url = userServiceUrl + "/api/v1/users/" + userId + "/push-tokens";
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return List.of();
            JsonNode node = objectMapper.readTree(json);
            JsonNode tokens = node.path("tokens");
            if (!tokens.isArray()) return List.of();
            List<String> out = new ArrayList<>(tokens.size());
            tokens.forEach(t -> out.add(t.asText()));
            return out;
        } catch (Exception e) {
            log.debug("Failed to fetch push tokens for {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    public record UserInfo(String email, String name, String phone) {}
}

