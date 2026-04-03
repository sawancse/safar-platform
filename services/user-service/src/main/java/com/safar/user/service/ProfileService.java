package com.safar.user.service;

import com.safar.user.dto.AdminHostDto;
import com.safar.user.dto.MyProfileDto;
import com.safar.user.dto.PublicHostDto;
import com.safar.user.dto.SyncProfileRequest;
import com.safar.user.dto.UpdateProfileRequest;
import com.safar.user.entity.HostSubscription;
import com.safar.user.entity.UserProfile;
import com.safar.user.repository.HostSubscriptionRepository;
import com.safar.user.repository.ProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final HostSubscriptionRepository hostSubscriptionRepository;

    @Cacheable(value = "users", key = "#userId")
    public MyProfileDto getMyProfile(UUID userId) {
        UserProfile profile = profileRepository.findById(userId)
                .orElse(UserProfile.builder().userId(userId).build());
        return toMyProfileDto(profile);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public MyProfileDto updateProfile(UUID userId, UpdateProfileRequest req) {
        UserProfile profile = profileRepository.findById(userId)
                .orElse(UserProfile.builder().userId(userId).build());

        if (req.name() != null) profile.setName(req.name());
        if (req.displayName() != null) profile.setDisplayName(req.displayName());
        if (req.email() != null) profile.setEmail(req.email());
        if (req.avatarUrl() != null) profile.setAvatarUrl(req.avatarUrl());
        if (req.language() != null) profile.setLanguage(req.language());
        if (req.phone() != null) profile.setPhone(req.phone());
        if (req.dateOfBirth() != null) profile.setDateOfBirth(req.dateOfBirth());
        if (req.gender() != null) profile.setGender(req.gender());
        if (req.nationality() != null) profile.setNationality(req.nationality());
        if (req.address() != null) profile.setAddress(req.address());
        if (req.passportName() != null) profile.setPassportName(req.passportName());
        if (req.passportNumber() != null) profile.setPassportNumber(req.passportNumber());
        if (req.passportExpiry() != null) profile.setPassportExpiry(req.passportExpiry());
        if (req.bio() != null) profile.setBio(req.bio());
        if (req.languages() != null) profile.setLanguages(req.languages());

        UserProfile saved = profileRepository.save(profile);
        calculateProfileCompletion(saved);
        return toMyProfileDto(saved);
    }

    public List<MyProfileDto> getAllProfiles() {
        return profileRepository.findAll().stream().map(this::toMyProfileDto).toList();
    }

    public List<AdminHostDto> getAllHostsForAdmin() {
        List<UserProfile> profiles = profileRepository.findAll().stream()
                .filter(p -> "HOST".equals(p.getRole()) || "BOTH".equals(p.getRole()) || "ADMIN".equals(p.getRole()))
                .toList();

        Map<UUID, HostSubscription> subscriptionMap = hostSubscriptionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(HostSubscription::getHostId, s -> s));

        return profiles.stream().map(p -> {
            HostSubscription sub = subscriptionMap.get(p.getUserId());
            String tier = sub != null ? sub.getTier().name() : "FREE";
            return new AdminHostDto(
                    p.getUserId(),
                    p.getName(),
                    p.getPhone(),
                    p.getEmail(),
                    p.getRole(),
                    tier,
                    null,
                    p.getAccountStatus() != null ? p.getAccountStatus() : "ACTIVE",
                    p.getSuspensionReason(),
                    p.getSuspendedAt(),
                    p.getCreatedAt()
            );
        }).toList();
    }

    @Transactional
    public void suspendHost(UUID hostId, UUID adminId, String reason) {
        UserProfile p = profileRepository.findById(hostId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Host not found: " + hostId));
        p.setAccountStatus("SUSPENDED");
        p.setSuspendedAt(java.time.OffsetDateTime.now());
        p.setSuspensionReason(reason);
        p.setSuspendedBy(adminId);
        profileRepository.save(p);
    }

    @Transactional
    public void unsuspendHost(UUID hostId) {
        UserProfile p = profileRepository.findById(hostId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Host not found: " + hostId));
        p.setAccountStatus("ACTIVE");
        p.setSuspendedAt(null);
        p.setSuspensionReason(null);
        p.setSuspendedBy(null);
        profileRepository.save(p);
    }

    @Transactional
    public void banHost(UUID hostId, UUID adminId, String reason) {
        UserProfile p = profileRepository.findById(hostId)
                .orElseThrow(() -> new java.util.NoSuchElementException("Host not found: " + hostId));
        p.setAccountStatus("BANNED");
        p.setSuspendedAt(java.time.OffsetDateTime.now());
        p.setSuspensionReason(reason);
        p.setSuspendedBy(adminId);
        profileRepository.save(p);
    }

    public List<AdminHostDto> getSuspendedHosts() {
        return profileRepository.findAll().stream()
                .filter(p -> "SUSPENDED".equals(p.getAccountStatus()) || "BANNED".equals(p.getAccountStatus()))
                .map(p -> new AdminHostDto(
                        p.getUserId(), p.getName(), p.getPhone(), p.getEmail(),
                        p.getRole(), null, null,
                        p.getAccountStatus(), p.getSuspensionReason(), p.getSuspendedAt(),
                        p.getCreatedAt()
                )).toList();
    }

    @Transactional
    public void syncFromAuth(UUID userId, SyncProfileRequest req) {
        UserProfile profile = profileRepository.findById(userId)
                .orElse(UserProfile.builder().userId(userId).build());

        if (req.name() != null) profile.setName(req.name());
        if (req.phone() != null) profile.setPhone(req.phone());
        if (req.role() != null) profile.setRole(req.role());

        profileRepository.save(profile);
    }

    @Transactional
    public void upgradeRole(UUID userId, String role) {
        UserProfile profile = profileRepository.findById(userId)
                .orElse(UserProfile.builder().userId(userId).build());
        profile.setRole(role);
        profileRepository.save(profile);
    }

    public PublicHostDto getPublicHostProfile(UUID hostId) {
        UserProfile profile = profileRepository.findById(hostId)
                .orElseThrow(() -> new NoSuchElementException("Host not found: " + hostId));
        return toPublicHostDto(profile);
    }

    private MyProfileDto toMyProfileDto(UserProfile p) {
        return new MyProfileDto(
                p.getUserId(), p.getName(), p.getDisplayName(), p.getEmail(),
                p.getAvatarUrl(), p.getPhone(), p.getRole(), p.getLanguage(),
                p.getDateOfBirth(), p.getGender(), p.getNationality(), p.getAddress(),
                p.getPassportName(), p.getPassportNumber(), p.getPassportExpiry(),
                p.getBio(), p.getLanguages(), p.getResponseRate(),
                p.getAvgResponseMinutes(), p.getTotalHostReviews(),
                p.getProfileCompletion(), p.getUpdatedAt());
    }

    private PublicHostDto toPublicHostDto(UserProfile p) {
        String trustBadge = deriveTrustBadge(p.getTrustScore());
        return new PublicHostDto(
                p.getUserId(), p.getName(), p.getAvatarUrl(),
                p.getBio(), p.getLanguages(), p.getVerificationLevel(),
                p.getTrustScore(), trustBadge, p.getResponseRate(),
                p.getAvgResponseMinutes(), p.getTotalHostReviews(),
                p.getHostType(), p.getSelfieVerified(),
                p.getCreatedAt(), p.getLastActiveAt()
        );
    }

    private String deriveTrustBadge(Integer trustScore) {
        if (trustScore == null) return "NEW";
        if (trustScore >= 90) return "SUPERHOST";
        if (trustScore >= 70) return "TRUSTED";
        if (trustScore >= 40) return "RISING";
        return "NEW";
    }

    @Transactional
    public String uploadAvatar(UUID userId, MultipartFile file) throws IOException {
        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));

        // Save file locally
        String filename = "avatar-" + userId + "-" + System.currentTimeMillis() + getExtension(file.getOriginalFilename());
        Path uploadDir = Path.of("uploads/avatars");
        Files.createDirectories(uploadDir);
        Path filePath = uploadDir.resolve(filename);
        file.transferTo(filePath.toFile());

        String avatarUrl = "/api/v1/users/avatars/" + filename;
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);

        // Recalculate profile completion
        calculateProfileCompletion(profile);

        return avatarUrl;
    }

    @Transactional
    public void setAvatarUrl(UUID userId, String avatarUrl) {
        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("Profile not found"));
        profile.setAvatarUrl(avatarUrl);
        profileRepository.save(profile);
        calculateProfileCompletion(profile);
    }

    private String getExtension(String filename) {
        if (filename == null) return ".jpg";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot) : ".jpg";
    }

    public int calculateProfileCompletion(UserProfile profile) {
        int score = 0;
        if (profile.getAvatarUrl() != null && !profile.getAvatarUrl().isBlank()) score += 20;
        if (profile.getName() != null && !profile.getName().isBlank()) score += 15;
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) score += 15;
        if (profile.getPhone() != null && !profile.getPhone().isBlank()) score += 15;
        if (profile.getBio() != null && !profile.getBio().isBlank()) score += 10;
        if (profile.getLanguages() != null && !profile.getLanguages().isBlank()) score += 10;
        // KYC verified = 15%
        if ("VERIFIED".equals(profile.getVerificationLevel())) score += 15;

        profile.setProfileCompletion(score);
        profileRepository.save(profile);
        return score;
    }
}
