package com.safar.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@Slf4j
public class UserClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String userServiceUrl;

    public UserClient(RestTemplate restTemplate,
                      ObjectMapper objectMapper,
                      @Value("${services.user.url}") String userServiceUrl) {
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
                    node.path("name").asText("")
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

    public record UserInfo(String email, String name) {}
}
