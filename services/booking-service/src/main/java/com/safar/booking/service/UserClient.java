package com.safar.booking.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-service client for user-service. Currently used only by the
 * TripIntentEvaluator to fetch user flags (e.g. medical_history,
 * new_pg_signup) for HISTORY/MEDICAL rule matching.
 *
 * Fail-soft: any error returns an empty Set, which means the rule simply
 * doesn't fire. Better to under-suggest than to break the booking flow.
 */
@Service
@Slf4j
public class UserClient {

    @Value("${services.user-service.url:http://localhost:8092}")
    private String userServiceUrl;

    private final RestClient restClient = RestClient.create();

    /**
     * Fetch the user's profile flags from user-service.
     *
     * Expected endpoint (NOT YET BUILT in user-service):
     *   GET /api/v1/users/{id}/flags → {"flags": ["medical_history", "new_pg_signup"]}
     *
     * Until that endpoint ships, this returns an empty Set so HISTORY
     * and MEDICAL rules silently no-op. The Trip evaluator's contract
     * is unaffected — when the endpoint ships, set FLIGHT/Trip-aware
     * rules just start firing automatically.
     */
    @SuppressWarnings("unchecked")
    public Set<String> getUserFlags(UUID userId) {
        if (userId == null) return Set.of();
        try {
            var resp = restClient.get()
                    .uri(userServiceUrl + "/api/v1/users/" + userId + "/flags")
                    .retrieve()
                    .body(Map.class);
            if (resp == null) return Set.of();
            List<String> flags = (List<String>) resp.get("flags");
            return flags == null ? Set.of() : new HashSet<>(flags);
        } catch (Exception e) {
            log.debug("getUserFlags({}) failed (non-fatal, falling back to empty): {}", userId, e.getMessage());
            return Set.of();
        }
    }
}
