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

    @Value("${services.user.url}")
    private String userServiceUrl;

    @Transactional
    public AuthResponse verifyOtpAndLogin(VerifyOtpRequest request) {
        if (!otpService.verifyOtp(request.phone(), request.otp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhone(request.phone())
                .orElseGet(() -> {
                    if (request.name() == null || request.name().isBlank()) {
                        throw new IllegalArgumentException("Name is required for new users");
                    }
                    return createUser(request.phone(), request.name());
                });

        syncProfileAsync(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse verifyEmailOtpAndLogin(VerifyEmailOtpRequest request) {
        if (!otpService.verifyOtp(request.email(), request.otp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseGet(() -> {
                    if (request.name() == null || request.name().isBlank()) {
                        throw new IllegalArgumentException("Name is required for new users");
                    }
                    return createEmailUser(request.email(), request.name());
                });

        syncProfileAsync(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse googleSignIn(GoogleSignInRequest request) {
        GoogleTokenVerifier.GoogleUserInfo info = googleTokenVerifier.verify(request.idToken());

        User user = userRepository.findByGoogleId(info.googleId())
                .orElseGet(() -> userRepository.findByEmail(info.email())
                        .map(existing -> {
                            existing.setGoogleId(info.googleId());
                            return userRepository.save(existing);
                        })
                        .orElseGet(() -> createGoogleUser(info)));

        syncProfileAsync(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse appleSignIn(AppleSignInRequest request) {
        AppleTokenVerifier.AppleUserInfo info = appleTokenVerifier.verify(
                request.identityToken(), request.name());

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

        syncProfileAsync(user);
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
        syncProfileAsync(user);
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

    private void syncProfileAsync(User user) {
        try {
            String url = userServiceUrl + "/api/v1/internal/users/" + user.getId() + "/sync-profile";
            Map<String, Object> body = new HashMap<>();
            body.put("name", user.getName());
            body.put("phone", user.getPhone());
            body.put("role", user.getRole().name());
            restTemplate.postForEntity(url, body, Void.class);
        } catch (Exception e) {
            log.warn("Failed to sync profile for user {}: {}", user.getId(), e.getMessage());
        }
    }

    private User createUser(String phone, String name) {
        return userRepository.save(User.builder()
                .phone(phone)
                .name(name)
                .role(UserRole.GUEST)
                .build());
    }

    private User createEmailUser(String email, String name) {
        return userRepository.save(User.builder()
                .email(email)
                .name(name)
                .role(UserRole.GUEST)
                .build());
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

    private UserDto toDto(User user) {
        return new UserDto(
                user.getId(), user.getPhone(), user.getEmail(),
                user.getName(), user.getRole().name(),
                user.getKycStatus().name(), user.getAvatarUrl(),
                user.getLanguage(), user.hasPassword()
        );
    }
}
