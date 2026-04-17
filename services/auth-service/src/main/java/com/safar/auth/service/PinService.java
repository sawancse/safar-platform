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

    // Weak PINs that are easily guessable
    private static final java.util.Set<String> WEAK_PINS = java.util.Set.of(
            "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
            "1234", "4321", "1212", "2121", "0123", "3210", "5678", "8765",
            "12345", "54321", "123456", "654321", "111111", "000000",
            "112233", "121212", "131313", "696969", "123123"
    );

    private final OtpService otpService;

    @Value("${services.user-service.url}")
    private String userServiceUrl;

    private void validatePinComplexity(String pin) {
        if (pin == null || !pin.matches("^\\d{4,6}$")) {
            throw new IllegalArgumentException("PIN must be 4-6 digits");
        }
        if (WEAK_PINS.contains(pin)) {
            throw new IllegalArgumentException("This PIN is too common. Please choose a stronger PIN.");
        }
        // Check all same digits
        if (pin.chars().distinct().count() == 1) {
            throw new IllegalArgumentException("PIN cannot be all the same digit");
        }
    }

    // ── Set PIN (first time, requires authenticated user) ──

    @Transactional
    public void setPin(UUID userId, SetPinRequest request) {
        validatePinComplexity(request.pin());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPinHash(pinEncoder.encode(request.pin()));
        user.setPinSetAt(OffsetDateTime.now());
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN set for user {}", userId);
        // TODO: send security notification when Kafka added to auth-service
    }

    // ── Change PIN (requires current PIN) ──

    @Transactional
    public void changePin(UUID userId, ChangePinRequest request) {
        validatePinComplexity(request.newPin());
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
        log.warn("SECURITY: PIN changed for user {}", userId);
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

    // ── Reset PIN via OTP (authenticated — user already logged in) ──

    @Transactional
    public void resetPinAfterOtp(UUID userId, SetPinRequest request) {
        validatePinComplexity(request.pin());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPinHash(pinEncoder.encode(request.pin()));
        user.setPinSetAt(OffsetDateTime.now());
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        log.info("PIN reset via OTP for user {}", userId);
        log.warn("SECURITY: PIN reset for user {}", userId);
    }

    // ── Forgot PIN (pre-auth — user not logged in, uses phone + OTP + new PIN) ──

    @Transactional
    public AuthResponse forgotPin(String phone, String otp, String newPin) {
        validatePinComplexity(newPin);

        // Verify OTP first
        boolean otpValid = otpService.verifyOtp(phone, otp);
        if (!otpValid) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found with this phone"));

        user.setPinHash(pinEncoder.encode(newPin));
        user.setPinSetAt(OffsetDateTime.now());
        user.setPinFailedAttempts(0);
        user.setPinLockedUntil(null);
        userRepository.save(user);

        // Auto-login after PIN reset
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        log.info("PIN reset via forgot-PIN for user {} (phone={})", user.getId(), phone);

        return new AuthResponse(
                accessToken, refreshToken, jwtService.getExpirySeconds(),
                new UserDto(user.getId(), user.getPhone(), user.getEmail(), user.getName(),
                        user.getRole().name(), user.getKycStatus().name(), user.getAvatarUrl(),
                        user.getLanguage(), user.hasPassword())
        );
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

    public boolean hasPin(String phone) {
        try {
            User user = userRepository.findByPhone(phone).orElse(null);
            return user != null && user.hasPin();
        } catch (Exception e) { return false; }
    }

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
