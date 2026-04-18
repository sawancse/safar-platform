package com.safar.auth.controller;

import com.safar.auth.entity.User;
import com.safar.auth.entity.enums.UserRole;
import com.safar.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @GetMapping("/users/{userId}")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable UUID userId) {
        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(Map.<String, Object>of(
                        "id", u.getId(),
                        "phone", u.getPhone() == null ? "" : u.getPhone(),
                        "email", u.getEmail() == null ? "" : u.getEmail(),
                        "name", u.getName() == null ? "" : u.getName())))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * One-shot backfill: push every auth user to user-service so any orphaned
     * accounts (signups from before sync was made synchronous) become visible
     * in the admin users list. Idempotent — sync-profile upserts by id.
     */
    @PostMapping("/users/backfill-profiles")
    public ResponseEntity<Map<String, Object>> backfillProfiles() {
        List<User> all = userRepository.findAll();
        int synced = 0, failed = 0;
        for (User u : all) {
            Map<String, Object> body = new HashMap<>();
            body.put("name", u.getName());
            body.put("phone", u.getPhone());
            body.put("email", u.getEmail());
            body.put("role", u.getRole() != null ? u.getRole().name() : UserRole.GUEST.name());
            try {
                restTemplate.postForEntity(
                        userServiceUrl + "/api/v1/internal/users/" + u.getId() + "/sync-profile",
                        body, Void.class);
                synced++;
            } catch (RuntimeException e) {
                failed++;
                log.warn("Backfill failed for {}: {}", u.getId(), e.getMessage());
            }
        }
        log.info("Profile backfill: {} synced, {} failed of {} total", synced, failed, all.size());
        return ResponseEntity.ok(Map.of("total", all.size(), "synced", synced, "failed", failed));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Void> upgradeRole(@PathVariable UUID userId,
                                             @RequestBody Map<String, String> body) {
        String newRole = body.get("role");
        if (newRole == null) {
            return ResponseEntity.badRequest().build();
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }

        UserRole targetRole = UserRole.valueOf(newRole);
        UserRole currentRole = user.getRole();

        // Don't downgrade ADMIN
        if (currentRole == UserRole.ADMIN) {
            log.debug("User {} is ADMIN, skipping role change to {}", userId, targetRole);
            return ResponseEntity.ok().build();
        }

        // Upgrade logic: GUEST→HOST, GUEST→BOTH, HOST→BOTH etc.
        if (currentRole != targetRole) {
            // If user is HOST and target is GUEST (or vice versa), set BOTH
            if ((currentRole == UserRole.HOST && targetRole == UserRole.GUEST)
                    || (currentRole == UserRole.GUEST && targetRole == UserRole.HOST)) {
                user.setRole(UserRole.HOST); // HOST can always book, so HOST covers both
            } else {
                user.setRole(targetRole);
            }
            userRepository.save(user);
            log.info("User {} role changed from {} to {}", userId, currentRole, user.getRole());
        }

        return ResponseEntity.ok().build();
    }
}
