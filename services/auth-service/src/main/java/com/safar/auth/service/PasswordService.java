package com.safar.auth.service;

import com.safar.auth.dto.*;
import com.safar.auth.entity.User;
import com.safar.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final OtpService otpService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;
    private static final Pattern UPPER_CASE = Pattern.compile("[A-Z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");
    // Dummy hash for timing-attack mitigation (bcrypt cost 12)
    private static final String DUMMY_HASH = "$2a$12$LJ3m4ys3Gzk0TQ2Yqr5xXOxqJz5Vz0V5k3VzG5yH5aK5sN5wU5a6";

    @Transactional
    public AuthResponse signInWithPassword(PasswordSignInRequest request) {
        User user = userRepository.findByEmail(request.email()).orElse(null);

        if (user == null) {
            // Timing attack mitigation: still run bcrypt comparison
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (user.isLocked()) {
            throw new IllegalStateException("Account is temporarily locked. Try again later.");
        }

        if (!user.hasPassword()) {
            // Timing attack mitigation
            passwordEncoder.matches(request.password(), DUMMY_HASH);
            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            int attempts = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(attempts);
            if (attempts >= MAX_FAILED_ATTEMPTS) {
                user.setLockedUntil(OffsetDateTime.now().plusMinutes(LOCK_MINUTES));
                userRepository.save(user);
                log.warn("Account locked for user {} after {} failed attempts", user.getId(), attempts);
                throw new IllegalStateException("Account is temporarily locked. Try again later.");
            }
            userRepository.save(user);
            throw new IllegalArgumentException("Invalid email or password");
        }

        // Successful login — reset counters
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        log.info("Password sign-in successful for user {}", user.getId());
        return buildAuthResponse(user);
    }

    @Transactional
    public void setPassword(UUID userId, SetPasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.hasPassword()) {
            throw new IllegalStateException("Password is already set. Use change-password instead.");
        }

        validatePasswordStrength(request.password());

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setPasswordSetAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Password set for user {}", userId);
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!user.hasPassword()) {
            throw new IllegalStateException("No password set. Use set-password first.");
        }

        if (!passwordEncoder.matches(request.oldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        validatePasswordStrength(request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordSetAt(OffsetDateTime.now());
        userRepository.save(user);

        log.info("Password changed for user {}", userId);
    }

    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        if (!otpService.verifyOtp(request.email(), request.otp())) {
            throw new IllegalArgumentException("Invalid or expired OTP");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validatePasswordStrength(request.newPassword());

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setPasswordSetAt(OffsetDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        log.info("Password reset for user {}", user.getId());
        return buildAuthResponse(user);
    }

    public CheckMethodResponse checkMethod(String email) {
        User user = userRepository.findByEmail(email).orElse(null);
        if (user == null) {
            return new CheckMethodResponse(false, false, List.of());
        }

        List<String> methods = new ArrayList<>();
        if (user.getEmail() != null) methods.add("EMAIL_OTP");
        if (user.getPhone() != null) methods.add("PHONE_OTP");
        if (user.hasPassword()) methods.add("PASSWORD");
        if (user.getGoogleId() != null) methods.add("GOOGLE");
        if (user.getAppleId() != null) methods.add("APPLE");

        return new CheckMethodResponse(true, user.hasPassword(), methods);
    }

    private void validatePasswordStrength(String password) {
        if (password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
        if (!UPPER_CASE.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one uppercase letter");
        }
        if (!DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("Password must contain at least one digit");
        }
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().name());
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
