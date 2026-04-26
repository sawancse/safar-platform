package com.safar.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.user.entity.UserPushToken;
import com.safar.user.repository.ProfileRepository;
import com.safar.user.repository.UserPushTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Cross-service flags + per-user push token management.
 *
 * Flags are read by booking-service's TripIntentEvaluator to drive
 * MEDICAL/HISTORY rule matching. Push tokens are read by
 * notification-service's PushNotificationService for Expo push delivery.
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Slf4j
public class UserFlagsPushController {

    private final ProfileRepository profileRepository;
    private final UserPushTokenRepository pushTokenRepository;
    private final ObjectMapper objectMapper;

    // ─── Flags (cross-service consumption) ──────────────────────

    @GetMapping("/{userId}/flags")
    public ResponseEntity<Map<String, Object>> getFlags(@PathVariable UUID userId) {
        var profile = profileRepository.findById(userId).orElse(null);
        List<String> flags = parseFlags(profile != null ? profile.getUserFlags() : null);
        return ResponseEntity.ok(Map.of("userId", userId, "flags", flags));
    }

    /** Add a flag idempotently. Used by other services to mark events like new_pg_signup. */
    @PostMapping("/{userId}/flags/{flag}")
    public ResponseEntity<Map<String, Object>> addFlag(@PathVariable UUID userId,
                                                        @PathVariable String flag) {
        var profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        List<String> flags = new ArrayList<>(parseFlags(profile.getUserFlags()));
        if (!flags.contains(flag)) {
            flags.add(flag);
            try {
                profile.setUserFlags(objectMapper.writeValueAsString(flags));
                profileRepository.save(profile);
            } catch (Exception e) {
                log.error("Failed to add flag {} for user {}: {}", flag, userId, e.getMessage());
                return ResponseEntity.status(500).build();
            }
        }
        return ResponseEntity.ok(Map.of("userId", userId, "flags", flags));
    }

    @DeleteMapping("/{userId}/flags/{flag}")
    public ResponseEntity<Map<String, Object>> removeFlag(@PathVariable UUID userId,
                                                           @PathVariable String flag) {
        var profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        List<String> flags = new ArrayList<>(parseFlags(profile.getUserFlags()));
        if (flags.remove(flag)) {
            try {
                profile.setUserFlags(objectMapper.writeValueAsString(flags));
                profileRepository.save(profile);
            } catch (Exception e) {
                log.error("Failed to remove flag {} for user {}: {}", flag, userId, e.getMessage());
                return ResponseEntity.status(500).build();
            }
        }
        return ResponseEntity.ok(Map.of("userId", userId, "flags", flags));
    }

    // ─── Push tokens (user-facing) ──────────────────────────────

    @PostMapping("/me/push-tokens")
    public ResponseEntity<UserPushToken> registerPushToken(
            Authentication auth,
            @RequestBody RegisterPushTokenRequest body) {
        UUID userId = UUID.fromString(auth.getName());
        // Upsert — same (user, token) → update lastUsedAt
        var existing = pushTokenRepository.findByUserIdAndPushToken(userId, body.pushToken());
        UserPushToken token = existing.orElseGet(() -> UserPushToken.builder()
                .userId(userId)
                .pushToken(body.pushToken())
                .platform(body.platform())
                .deviceId(body.deviceId())
                .build());
        token.setLastUsedAt(Instant.now());
        token.setPlatform(body.platform());
        if (body.deviceId() != null) token.setDeviceId(body.deviceId());
        return ResponseEntity.status(existing.isPresent() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(pushTokenRepository.save(token));
    }

    @GetMapping("/me/push-tokens")
    public ResponseEntity<List<UserPushToken>> myPushTokens(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(pushTokenRepository.findByUserId(userId));
    }

    /** Cross-service lookup — used by notification-service to send push. */
    @GetMapping("/{userId}/push-tokens")
    public ResponseEntity<Map<String, Object>> getPushTokens(@PathVariable UUID userId) {
        List<String> tokens = pushTokenRepository.findByUserId(userId).stream()
                .map(UserPushToken::getPushToken)
                .toList();
        return ResponseEntity.ok(Map.of("userId", userId, "tokens", tokens));
    }

    @DeleteMapping("/me/push-tokens/{id}")
    public ResponseEntity<Void> deletePushToken(Authentication auth, @PathVariable UUID id) {
        UUID userId = UUID.fromString(auth.getName());
        var existing = pushTokenRepository.findById(id).orElse(null);
        if (existing == null || !existing.getUserId().equals(userId)) {
            return ResponseEntity.notFound().build();
        }
        pushTokenRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    // ─── helpers ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private List<String> parseFlags(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (Exception e) {
            log.warn("Bad user_flags JSON: {}", e.getMessage());
            return List.of();
        }
    }

    public record RegisterPushTokenRequest(String pushToken, String platform, String deviceId) {}
}
