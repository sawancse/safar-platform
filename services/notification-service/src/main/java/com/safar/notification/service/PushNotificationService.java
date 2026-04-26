package com.safar.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Expo Push Notifications for the Safar mobile app (React Native + Expo SDK 51).
 *
 * Mobile-side dependency (NOT YET WIRED at the time of writing):
 *   1. Mobile app collects ExponentPushToken on first launch via expo-notifications
 *   2. POSTs token to user-service (e.g. PUT /api/v1/users/me/push-token)
 *   3. user-service persists in users.user_push_tokens (table does not yet exist)
 *   4. notification-service queries user-service for the token list before sending
 *
 * Until step 1-3 land, this service finds no token and silently no-ops. The
 * Expo HTTP send code path is fully functional — it activates the moment
 * tokens start being captured.
 *
 * Expo Push API: https://docs.expo.dev/push-notifications/sending-notifications/
 */
@Service
@Slf4j
public class PushNotificationService {

    @Value("${expo.push.url:https://exp.host/--/api/v2/push/send}")
    private String pushUrl;

    @Value("${expo.push.access-token:}")
    private String accessToken;          // Optional; required only if Expo project enables enhanced security

    private final UserClient userClient;
    private final RestTemplate restTemplate = new RestTemplate();

    public PushNotificationService(UserClient userClient) {
        this.userClient = userClient;
    }

    /**
     * Abandoned-search recovery push notification — fired by the
     * FlightSearchAbandonedConsumer on every pulse (1h/6h/24h).
     * Push has the highest engagement of all channels for app users
     * (~30-40% open rate vs 15-30% email).
     */
    public void sendFlightSearchAbandoned(UUID userId, String route, String date, String fareHint) {
        if (userId == null) return;
        List<String> tokens = lookupTokens(userId);
        if (tokens.isEmpty()) {
            log.debug("No push tokens for user {} — skipping push reminder", userId);
            return;
        }
        String body = "Still going " + route + " on " + date + "?"
                + (fareHint != null && !fareHint.isBlank() ? " " + fareHint : "");
        Map<String, Object> data = Map.of(
                "type", "FLIGHT_SEARCH_ABANDONED",
                "route", route,
                "date", date,
                "deeplink", "ysafar://flights"
        );
        for (String token : tokens) {
            sendOne(token, "Complete your flight search", body, data);
        }
        log.info("Sent {} push reminders for abandoned search to user {}", tokens.size(), userId);
    }

    // ── Token lookup ────────────────────────────────────────────

    private List<String> lookupTokens(UUID userId) {
        try {
            return userClient.getPushTokens(userId);
        } catch (Exception e) {
            log.debug("Push token lookup failed for {}: {}", userId, e.getMessage());
            return List.of();
        }
    }

    // ── Core Expo sender ────────────────────────────────────────

    private void sendOne(String token, String title, String body, Map<String, Object> data) {
        if (token == null || token.isBlank()) return;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));
            if (accessToken != null && !accessToken.isBlank()) {
                headers.setBearerAuth(accessToken);
            }

            // Expo Push payload
            Map<String, Object> message = Map.of(
                    "to", token,
                    "title", title,
                    "body", body,
                    "data", data,
                    "sound", "default",
                    "priority", "high",
                    "channelId", "default"
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(message, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(pushUrl, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Expo push failed: {} {}", response.getStatusCode(), response.getBody());
            }
        } catch (Exception e) {
            log.warn("Expo push send failed: {}", e.getMessage());
        }
    }
}
