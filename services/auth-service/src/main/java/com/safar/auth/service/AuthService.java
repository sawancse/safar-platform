package com.safar.auth.service;

import com.safar.auth.dto.AppleSignInRequest;
import com.safar.auth.dto.AuthResponse;
import com.safar.auth.dto.GoogleSignInRequest;
import com.safar.auth.dto.UserDto;
import com.safar.auth.dto.VerifyEmailOtpRequest;
import com.safar.auth.dto.VerifyOtpRequest;
import com.safar.auth.entity.User;
import com.safar.auth.entity.enums.UserRole;
import com.safar.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final OtpService          otpService;
    private final JwtService          jwtService;
    private final RestTemplate        restTemplate;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final AppleTokenVerifier  appleTokenVerifier;
    private final org.springframework.data.redis.core.StringRedisTemplate redis;

    private static final String DEVICE_PREFIX = "trusted:device:";
    private static final long DEVICE_TRUST_DAYS = 90;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        if (!otpService.verifyOtp(request.phone(), request.otp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhone(request.phone()).orElse(null);
        boolean isNew = (user == null);
        if (isNew) {
            if (request.name() == null || request.name().isBlank()) {
                throw new IllegalArgumentException("Name is required for new users");
            }
            user = signupNewUserWithPhone(request.phone(), request.name(),
                    normalizeEmail(request.email()));
        }

        syncProfile(user, isNew);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse verifyEmailOtpAndLogin(VerifyEmailOtpRequest request) {
        if (!otpService.verifyOtp(request.email(), request.otp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        String email = normalizeEmail(request.email());
        User user = userRepository.findByEmailIgnoreCase(email).orElse(null);
        boolean isNew = (user == null);
        if (isNew) {
            if (request.name() == null || request.name().isBlank()) {
                throw new IllegalArgumentException("Name is required for new users");
            }
            user = signupNewUserWithEmail(email, request.name(),
                    normalizePhone(request.phone()));
        }

        syncProfile(user, isNew);
        return buildAuthResponse(user);
    }

    private User signupNewUserWithPhone(String phone, String name, String email) {
        // Strict: new user must supply both phone and email so the (phone, email) pair
        // identifies one person. Prevents the "one phone across multiple emails" dupe.
        if (email == null) {
            throw new IllegalArgumentException(
                    "Email is required to create a new account. Please provide your email address.");
        }

        // Cross-check email (case-insensitive, matches LOWER(email) unique index)
        User existingByEmail = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existingByEmail != null) {
            if (existingByEmail.getPhone() != null && !existingByEmail.getPhone().equals(phone)) {
                throw new DuplicateAccountException(
                        "This email is already registered with another phone number. "
                                + "Please sign in using email OTP instead.");
            }
            existingByEmail.setPhone(phone);
            log.info("Linked verified phone {} to existing email account {}", phone, existingByEmail.getId());
            return userRepository.save(existingByEmail);
        }

        return userRepository.save(User.builder()
                .phone(phone)
                .email(email)
                .name(name)
                .role(UserRole.GUEST)
                .build());
    }

    private User signupNewUserWithEmail(String email, String name, String phone) {
        if (phone == null) {
            throw new IllegalArgumentException(
                    "Phone number is required to create a new account. Please provide your phone number.");
        }

        User existingByPhone = userRepository.findByPhone(phone).orElse(null);
        if (existingByPhone != null) {
            if (existingByPhone.getEmail() != null
                    && !existingByPhone.getEmail().equalsIgnoreCase(email)) {
                throw new DuplicateAccountException(
                        "This phone number is already registered with another email. "
                                + "Please sign in using phone OTP instead.");
            }
            existingByPhone.setEmail(email);
            log.info("Linked verified email {} to existing phone account {}", email, existingByPhone.getId());
            return userRepository.save(existingByPhone);
        }

        return userRepository.save(User.builder()
                .email(email)
                .phone(phone)
                .name(name)
                .role(UserRole.GUEST)
                .build());
    }

    private static String normalizeEmail(String email) {
        if (email == null) return null;
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String trimmed = phone.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @Transactional
    public AuthResponse googleSignIn(GoogleSignInRequest request) {
        GoogleTokenVerifier.GoogleUserInfo info = googleTokenVerifier.verify(request.idToken());

        boolean existedByGoogle = userRepository.findByGoogleId(info.googleId()).isPresent();
        boolean existedByEmail  = !existedByGoogle && userRepository.findByEmail(info.email()).isPresent();

        User user = userRepository.findByGoogleId(info.googleId())
                .orElseGet(() -> userRepository.findByEmail(info.email())
                        .map(existing -> {
                            existing.setGoogleId(info.googleId());
                            return userRepository.save(existing);
                        })
                        .orElseGet(() -> createGoogleUser(info)));

        boolean isNew = !existedByGoogle && !existedByEmail;
        syncProfile(user, isNew);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse appleSignIn(AppleSignInRequest request) {
        AppleTokenVerifier.AppleUserInfo info = appleTokenVerifier.verify(
                request.identityToken(), request.name());

        boolean existedByApple = userRepository.findByAppleId(info.appleId()).isPresent();
        boolean existedByEmail = !existedByApple && info.email() != null
                && userRepository.findByEmail(info.email()).isPresent();

        User user = userRepository.findByAppleId(info.appleId())
                .orElseGet(() -> {
                    // Check if email already exists (link accounts)
                    if (info.email() != null) {
                        return userRepository.findByEmail(info.email())
                                .map(existing -> {
                                    existing.setAppleId(info.appleId());
                                    return userRepository.save(existing);
                                })
                                .orElseGet(() -> createAppleUser(info));
                    }
                    return createAppleUser(info);
                });

        boolean isNew = !existedByApple && !existedByEmail;
        syncProfile(user, isNew);
        return buildAuthResponse(user);
    }

    private User createAppleUser(AppleTokenVerifier.AppleUserInfo info) {
        return userRepository.save(User.builder()
                .name(info.name() != null ? info.name() : "Apple User")
                .email(info.email())
                .appleId(info.appleId())
                .role(UserRole.GUEST)
                .build());
    }

    private User createGoogleUser(GoogleTokenVerifier.GoogleUserInfo info) {
        return userRepository.save(User.builder()
                .name(info.name() != null ? info.name() : info.email())
                .email(info.email())
                .googleId(info.googleId())
                .avatarUrl(info.pictureUrl())
                .role(UserRole.GUEST)
                .build());
    }

    public AuthResponse refreshTokens(String refreshToken) {
        UUID userId = jwtService.validateRefreshToken(refreshToken);
        jwtService.invalidateRefreshToken(refreshToken);  // rotate token

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return buildAuthResponse(user);
    }

    public void logout(String refreshToken) {
        jwtService.invalidateRefreshToken(refreshToken);
    }

    public void logoutAll(UUID userId) {
        jwtService.invalidateAllRefreshTokens(userId);
    }

    public void logoutAllFromRefreshToken(String refreshToken) {
        UUID userId = jwtService.validateRefreshToken(refreshToken);
        jwtService.invalidateAllRefreshTokens(userId);
    }

    /**
     * Trust a device for a user. Stores device fingerprint in Redis with 90-day TTL.
     * Key format: trusted:device:{userId}:{fingerprint} = deviceName
     */
    public void trustDevice(UUID userId, String deviceFingerprint, String deviceName) {
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) return;
        String key = DEVICE_PREFIX + userId + ":" + deviceFingerprint;
        redis.opsForValue().set(key, deviceName != null ? deviceName : "Unknown device",
                java.time.Duration.ofDays(DEVICE_TRUST_DAYS));
        log.info("Device trusted for user {}: {}", userId, deviceName);
    }

    /**
     * Check if device is trusted and login without OTP.
     * Returns AuthResponse if trusted, throws if not.
     */
    public AuthResponse loginWithTrustedDevice(String deviceFingerprint, String phone, String email) {
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) {
            throw new IllegalArgumentException("Device fingerprint required");
        }

        // Find user by phone or email
        User user = null;
        if (phone != null && !phone.isBlank()) {
            user = userRepository.findByPhone(phone).orElse(null);
        }
        if (user == null && email != null && !email.isBlank()) {
            user = userRepository.findByEmail(email).orElse(null);
        }
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }

        // Check if device is trusted for this user
        String key = DEVICE_PREFIX + user.getId() + ":" + deviceFingerprint;
        String deviceName = redis.opsForValue().get(key);
        if (deviceName == null) {
            throw new IllegalArgumentException("Device not trusted");
        }

        // Refresh the TTL on trusted device
        redis.expire(key, java.time.Duration.ofDays(DEVICE_TRUST_DAYS));

        log.info("Trusted device login for user {} on {}", user.getId(), deviceName);
        syncProfile(user, false);
        return buildAuthResponse(user);
    }

    /**
     * Remove a trusted device.
     */
    public void removeTrustedDevice(UUID userId, String deviceFingerprint) {
        if (deviceFingerprint == null || deviceFingerprint.isBlank()) return;
        String key = DEVICE_PREFIX + userId + ":" + deviceFingerprint;
        redis.delete(key);
        log.info("Device untrusted for user {}", userId);
    }

    /**
     * List all trusted devices for a user.
     * Scans Redis for keys matching trusted:device:{userId}:*
     */
    public List<Map<String, String>> listTrustedDevices(UUID userId) {
        String pattern = DEVICE_PREFIX + userId + ":*";
        java.util.Set<String> keys = redis.keys(pattern);
        List<Map<String, String>> devices = new java.util.ArrayList<>();
        if (keys != null) {
            String prefix = DEVICE_PREFIX + userId + ":";
            for (String key : keys) {
                String fingerprint = key.substring(prefix.length());
                String name = redis.opsForValue().get(key);
                Long ttl = redis.getExpire(key, java.util.concurrent.TimeUnit.DAYS);
                devices.add(Map.of(
                        "fingerprint", fingerprint,
                        "deviceName", name != null ? name : "Unknown",
                        "expiresInDays", ttl != null ? String.valueOf(ttl) : "?"
                ));
            }
        }
        return devices;
    }

    /**
     * Revoke all trusted devices for a user.
     */
    public void revokeAllDevices(UUID userId) {
        String pattern = DEVICE_PREFIX + userId + ":*";
        java.util.Set<String> keys = redis.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redis.delete(keys);
            log.info("All trusted devices revoked for user {}", userId);
        }
    }

    /**
     * Propagate the auth user to user-service so a UserProfile row exists.
     * Retries up to 3× with short backoff. If {@code newUser} is true, a final
     * failure throws — rolling back the @Transactional signup so we never leave
     * an orphan auth user invisible to admin. For existing users, failure is
     * logged but swallowed (profile already exists, the update is best-effort).
     */
    private void syncProfile(User user, boolean newUser) {
        String url = userServiceUrl + "/api/v1/internal/users/" + user.getId() + "/sync-profile";
        Map<String, Object> body = new HashMap<>();
        body.put("name", user.getName());
        body.put("phone", user.getPhone());
        body.put("email", user.getEmail());
        body.put("role", user.getRole().name());

        RuntimeException last = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                restTemplate.postForEntity(url, body, Void.class);
                return;
            } catch (RuntimeException e) {
                last = e;
                log.warn("Profile sync attempt {}/3 failed for user {}: {}", attempt, user.getId(), e.getMessage());
                if (attempt < 3) {
                    try { Thread.sleep(200L * attempt); }
                    catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
                }
            }
        }
        if (newUser) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Profile service is unavailable — cannot complete signup. Please try again shortly.", last);
        }
        log.error("Profile sync exhausted for existing user {} — continuing login", user.getId(), last);
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getExpirySeconds(),
                toDto(user)
        );
    }

    /**
     * Admin impersonation: generate tokens for a target user (for support purposes).
     * The JWT includes an "impersonatedBy" claim so actions can be audited.
     */
    public AuthResponse impersonate(UUID adminId, UUID targetUserId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new IllegalArgumentException("Admin user not found"));
        if (admin.getRole() != UserRole.ADMIN) {
            throw new SecurityException("Only ADMIN users can impersonate");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found: " + targetUserId));

        String accessToken = jwtService.generateImpersonationToken(target.getId(), target.getRole().name(), adminId);
        String refreshToken = jwtService.generateRefreshToken(target.getId());

        log.info("Admin {} impersonating user {} ({})", adminId, targetUserId, target.getName());

        return new AuthResponse(accessToken, refreshToken, jwtService.getExpirySeconds(), toDto(target));
    }

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(), user.getPhone(), user.getEmail(),
                user.getName(), user.getRole().name(),
                user.getKycStatus().name(), user.getAvatarUrl(),
                user.getLanguage(), user.hasPassword()
        );
    }
}
