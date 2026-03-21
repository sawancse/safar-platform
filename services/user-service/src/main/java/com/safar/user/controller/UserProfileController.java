package com.safar.user.controller;

import com.safar.user.dto.*;
import com.safar.user.service.HostSubscriptionService;
import com.safar.user.service.LanguageService;
import com.safar.user.service.ProfileService;
import com.safar.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final ProfileService profileService;
    private final HostSubscriptionService hostSubscriptionService;
    private final LanguageService languageService;

    // ── My profile ───────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<MyProfileDto> getMyProfile(Authentication auth) {
    	System.out.println("auth " + auth.toString());
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(profileService.getMyProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<MyProfileDto> updateMyProfile(Authentication auth,
                                                         @Valid @RequestBody UpdateProfileRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(profileService.updateProfile(userId, req));
    }

    // ── Avatar upload & serving ──────────────────────────────────────────────

    @PostMapping("/me/avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            Authentication auth) throws IOException {
        UUID userId = UUID.fromString(auth.getName());
        String avatarUrl = profileService.uploadAvatar(userId, file);
        return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
    }

    @GetMapping("/avatars/{filename}")
    public ResponseEntity<Resource> serveAvatar(@PathVariable String filename) {
        Path file = Path.of("uploads/avatars").resolve(filename);
        Resource resource = new FileSystemResource(file);
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .header("Cache-Control", "public, max-age=86400")
                .body(resource);
    }

    // ── Language preference ──────────────────────────────────────────────────

    @PutMapping("/me/language")
    public ResponseEntity<Map<String, String>> updateLanguage(Authentication auth,
                                                               @RequestBody Map<String, String> body) {
        UUID userId = UUID.fromString(auth.getName());
        String language = body.get("language");
        String updated = languageService.updateLanguage(userId, language);
        return ResponseEntity.ok(Map.of("preferredLanguage", updated));
    }

    // ── Taste profile ────────────────────────────────────────────────────────

    @GetMapping("/me/taste-profile")
    public ResponseEntity<TasteProfileDto> getProfile(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(userProfileService.getProfile(userId));
    }

    @PutMapping("/me/taste-profile")
    public ResponseEntity<TasteProfileDto> upsertProfile(Authentication auth,
                                                          @RequestBody UpdateTasteProfileRequest req) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(userProfileService.upsertProfile(userId, req));
    }

    // ── Public host profile ──────────────────────────────────────────────────

    @GetMapping("/hosts/{hostId}")
    public ResponseEntity<PublicHostDto> getHostProfile(@PathVariable UUID hostId) {
        return ResponseEntity.ok(profileService.getPublicHostProfile(hostId));
    }
}
