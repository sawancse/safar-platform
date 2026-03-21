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
        if (user.getRole() == UserRole.GUEST) {
            user.setRole(targetRole);
            userRepository.save(user);
            log.info("User {} role upgraded from GUEST to {}", userId, targetRole);
        }

        return ResponseEntity.ok().build();
    }
}
