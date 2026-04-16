package com.safar.auth.controller;

import com.safar.auth.entity.User;
import com.safar.auth.entity.enums.UserRole;
import com.safar.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/internal")
@RequiredArgsConstructor
public class InternalAuthController {

    private final UserRepository userRepository;

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
