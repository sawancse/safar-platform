package com.safar.auth.service;

import com.safar.auth.dto.*;
import com.safar.auth.entity.User;
import com.safar.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * HDFC-style PIN login.
 * - User sets a 4-6 digit PIN after OTP verification
 * - PIN replaces OTP for future logins (quick login)
 * - 5 wrong attempts → 15 min lock
 * - PIN stored as BCrypt hash (same as password)
 * - Reset PIN via OTP (forgot PIN flow)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PinService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthService authService;

    private final BCryptPasswordEncoder pinEncoder = new BCryptPasswordEncoder(10);

    private static final int MAX_PIN_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    // ── Set PIN (first time, requires authenticated user) ──

    @Transactional
    public void setPin(UUID userId, SetPinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPinHash(pinEncoder.encode(request.pin()));
        user.setPinSetAt(OffsetDateTime.now());
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN set for user {}", userId);
    }

    // ── Change PIN (requires current PIN) ──

    @Transactional
    public void changePin(UUID userId, ChangePinRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.hasPin()) {
            throw new IllegalStateException("No PIN set. Use set-pin first.");
        }

        if (!pinEncoder.matches(request.currentPin(), user.getPinHash())) {
            throw new IllegalArgumentException("Current PIN is incorrect");
        }

        user.setPinHash(pinEncoder.encode(request.newPin()));
        user.setPinSetAt(OffsetDateTime.now());
        user.setPinFailedAttempts(0);
        userRepository.save(user);

        log.info("PIN changed for user {}", userId);
    }

    // ── Sign in with PIN (replaces OTP) ──

    @Transactional
    public AuthResponse signInWithPin(PinSignInRequest request) {
        User user = findUser(request.phone(), request.email());

        if (!user.hasPin()) {
            throw new IllegalStateException("No PIN set for this account. Please login with OTP first and set a PIN.");
        }

        if (user.isPinLocked()) {
            long minutesLeft = java.time.Duration.between(OffsetDateTime.now(), user.getPinLockedUntil()).toMinutes() + 1;
            throw new IllegalStateException("Account locked due to too many failed attempts. Try again in " + minutesLeft + " minutes.");
        }

        if (!pinEncoder.matches(request.pin(), user.getPinHash())) {
            int attempts = user.getPinFailedAttempts() + 1;
            user.setPinFailedAttempts(attempts);

            if (attempts >= MAX_PIN_ATTEMPTS) {
                user.setPinLockedUntil(OffsetDateTime.now().plusMinutes(LOCK_MINUTES));
                userRepository.save(user);
                log.warn("PIN locked for user {} after {} attempts", user.getId(), attempts);
                throw new IllegalStateException("Too many failed attempts. Account locked for " + LOCK_MINUTES + " minutes.");
            }

            userRepository.save(user);
            int remaining = MAX_PIN_ATTEMPTS - attempts;
            throw new IllegalArgumentException("Incorrect PIN. " + remaining + " attempt(s) remaining.");
        }

        // Success — reset attempts
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        // Generate tokens (same as OTP login)
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("PIN login successful for user {}", user.getId());

        return new AuthResponse(
                accessToken,
                refreshToken,
                jwtService.getExpirySeconds(),
                new UserDto(user.getId(), user.getPhone(), user.getEmail(), user.getName(),
                        user.getRole().name(), user.getKycStatus().name(), user.getAvatarUrl(),
                        user.getLanguage(), user.hasPassword())
        );
    }

    // ── Reset PIN via OTP (forgot PIN) ──

    @Transactional
    public void resetPinAfterOtp(UUID userId, SetPinRequest request) {
        // Called after OTP verification — userId comes from the JWT issued by OTP login
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPinHash(pinEncoder.encode(request.pin()));
        user.setPinSetAt(OffsetDateTime.now());
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN reset via OTP for user {}", userId);
    }

    // ── Remove PIN ──

    @Transactional
    public void removePin(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPinHash(null);
        user.setPinSetAt(null);
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN removed for user {}", userId);
    }

    // ── Check if user has PIN set ──

    public Map<String, Object> checkPinStatus(String phone, String email) {
        try {
            User user = findUser(phone, email);
            log.info("PIN check for {}: hasPin={}, pinLocked={}", phone != null ? phone : email, user.hasPin(), user.isPinLocked());
            return Map.of(
                    "hasPin", user.hasPin(),
                    "pinLocked", user.isPinLocked()
            );
        } catch (Exception e) {
            log.warn("PIN check failed for {}: {}", phone != null ? phone : email, e.getMessage());
            return Map.of("hasPin", false, "pinLocked", false);
        }
    }

    private User findUser(String phone, String email) {
        if (phone != null && !phone.isBlank()) {
            return userRepository.findByPhone(phone)
                    .orElseThrow(() -> new RuntimeException("User not found with this phone number"));
        }
        if (email != null && !email.isBlank()) {
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with this email"));
        }
        throw new IllegalArgumentException("Phone or email is required");
    }
}
