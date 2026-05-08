package com.safar.services.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

/**
 * Thin client around user-service so vendor onboarding can pull the
 * registered phone/email off the auth user's profile when stamping a
 * PartnerVendor stub. Mirrors notification-service's UserClient — fail-soft
 * on every error so vendor approval never blocks on a flaky user-service.
 */
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

    public UserInfo getUser(UUID userId) {
        if (userId == null) return null;
        try {
            String url = userServiceUrl + "/api/v1/internal/users/" + userId + "/email";
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return null;
            JsonNode node = objectMapper.readTree(json);
            return new UserInfo(
                    node.path("email").asText(""),
                    node.path("name").asText(""),
                    node.path("phone").asText("")
            );
        } catch (Exception e) {
            log.warn("UserClient lookup failed for {}: {}", userId, e.getMessage());
            return null;
        }
    }

    public record UserInfo(String email, String name, String phone) {
        public boolean hasPhone() { return phone != null && !phone.isBlank(); }
        public boolean hasEmail() { return email != null && !email.isBlank(); }
    }
}
