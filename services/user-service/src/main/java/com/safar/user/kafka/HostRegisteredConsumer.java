package com.safar.user.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class HostRegisteredConsumer {

    private final ProfileService profileService;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    @Value("${services.auth-service.url}")
    private String authServiceUrl;

    @KafkaListener(topics = "host.registered", groupId = "user-service")
    public void onHostRegistered(String message) {
        try {
            JsonNode node = objectMapper.readTree(message);
            UUID hostId = UUID.fromString(node.get("hostId").asText());

            // Update role in user-service profiles table
            profileService.upgradeRole(hostId, "HOST");

            // Update role in auth-service
            String url = authServiceUrl + "/api/v1/internal/users/" + hostId + "/role";
            restTemplate.put(url, Map.of("role", "HOST"));

            log.info("Host {} role upgraded to HOST", hostId);
        } catch (Exception e) {
            log.error("Failed to process host.registered event: {}", e.getMessage(), e);
        }
    }
}
