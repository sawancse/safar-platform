package com.safar.auth.controller;

import com.safar.auth.dto.*;
import com.safar.auth.repository.UserRepository;
import com.safar.auth.service.AuthService;
import com.safar.auth.service.OtpService;
import com.safar.auth.service.PasswordService;
import com.safar.auth.service.PinService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final OtpService      otpService;
    private final AuthService     authService;
    private final PasswordService passwordService;
    private final PinService      pinService;
    private final UserRepository  userRepository;

    @PostMapping("/otp/send")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        boolean delivered = otpService.sendOtp(request.phone());
        boolean hasPin = pinService.hasPin(request.phone());
        boolean hasPassword = passwordService.hasPassword(request.phone());

        Map<String, Object> response = new java.util.LinkedHashMap<>();
        response.put("delivered", delivered);
        response.put("hasPin", hasPin);
        response.put("hasPassword", hasPassword);

        if (!delivered && (hasPin || hasPassword)) {
            response.put("fallback", hasPin ? "PIN" : "PASSWORD");
            response.put("message", "SMS delivery failed. You can login with your " + (hasPin ? "PIN" : "password") + " instead.");
        } else if (!delivered) {
            response.put("fallback", "NONE");
            response.put("message", "SMS delivery failed. Please try again or use email OTP.");
        } else {
            response.put("message", "OTP sent successfully");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/otp/verify")
    public ResponseEntity<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {
        return ResponseEntity.ok(authService.verifyOtpAndLogin(request));
    }

    @PostMapping("/otp/email/send")
    public ResponseEntity<Void> sendEmailOtp(
            @Valid @RequestBody SendEmailOtpRequest request) {
        otpService.sendEmailOtp(request.email());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/otp/email/verify")
    public ResponseEntity<AuthResponse> verifyEmailOtp(
            @Valid @RequestBody VerifyEmailOtpRequest request) {
        return ResponseEntity.ok(authService.verifyEmailOtpAndLogin(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshTokens(request.refreshToken()));
    }

    @PostMapping("/google/signin")
    public ResponseEntity<AuthResponse> googleSignIn(
            @Valid @RequestBody GoogleSignInRequest request) {
        return ResponseEntity.ok(authService.googleSignIn(request));
    }

    @PostMapping("/apple/signin")
    public ResponseEntity<AuthResponse> appleSignIn(
            @Valid @RequestBody AppleSignInRequest request) {
        return ResponseEntity.ok(authService.appleSignIn(request));
    }

    @PostMapping("/otp/whatsapp/send")
    public ResponseEntity<Void> sendWhatsAppOtp(
            @Valid @RequestBody SendWhatsAppOtpRequest request) {
        otpService.sendWhatsAppOtp(request.phone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody RefreshTokenRequest request) {
        authService.logout(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestBody RefreshTokenRequest request) {
        authService.logoutAllFromRefreshToken(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/device/trust")
    public ResponseEntity<Map<String, String>> trustDevice(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") UUID userId) {
        String deviceFingerprint = body.get("deviceFingerprint");
        String deviceName = body.get("deviceName");
        authService.trustDevice(userId, deviceFingerprint, deviceName);
        return ResponseEntity.ok(Map.of("status", "trusted"));
    }

    @PostMapping("/device/check")
    public ResponseEntity<AuthResponse> checkTrustedDevice(
            @RequestBody Map<String, String> body) {
        String deviceFingerprint = body.get("deviceFingerprint");
        String phone = body.get("phone");
        String email = body.get("email");
        AuthResponse response = authService.loginWithTrustedDevice(deviceFingerprint, phone, email);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/device/trust")
    public ResponseEntity<Void> removeTrustedDevice(
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") UUID userId) {
        String deviceFingerprint = body.get("deviceFingerprint");
        authService.removeTrustedDevice(userId, deviceFingerprint);
        return ResponseEntity.noContent().build();
    }

    /** List all trusted devices for the current user */
    @GetMapping("/devices")
    public ResponseEntity<java.util.List<Map<String, String>>> listDevices(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(authService.listTrustedDevices(userId));
    }

    /** Revoke all trusted devices (sign out everywhere) */
    @DeleteMapping("/devices/all")
    public ResponseEntity<Void> revokeAllDevices(
            @RequestHeader("X-User-Id") UUID userId) {
        authService.revokeAllDevices(userId);
        return ResponseEntity.noContent().build();
    }

    // ── Password authentication endpoints ──────────────────────────────

    @PostMapping("/password/signin")
    public ResponseEntity<AuthResponse> passwordSignIn(
            @Valid @RequestBody PasswordSignInRequest request) {
        return ResponseEntity.ok(passwordService.signInWithPassword(request));
    }

    @PostMapping("/password/set")
    public ResponseEntity<Void> setPassword(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SetPasswordRequest request) {
        passwordService.setPassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/change")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/password/reset")
    public ResponseEntity<AuthResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordService.resetPassword(request));
    }

    @GetMapping("/check-method")
    public ResponseEntity<CheckMethodResponse> checkMethod(
            @RequestParam String email) {
        return ResponseEntity.ok(passwordService.checkMethod(email));
    }

    /**
     * Lightweight existence check — used by the signup UI to validate a second
     * identifier (phone/email) BEFORE the user finishes OTP verification, so
     * they can be warned early if it already belongs to someone else.
     */
    @GetMapping("/identifier/exists")
    public ResponseEntity<Map<String, Boolean>> identifierExists(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        Map<String, Boolean> out = new java.util.LinkedHashMap<>();
        if (phone != null && !phone.isBlank()) {
            out.put("phone", userRepository.findByPhone(phone.trim()).isPresent());
        }
        if (email != null && !email.isBlank()) {
            out.put("email", userRepository.findByEmail(email.trim().toLowerCase()).isPresent());
        }
        return ResponseEntity.ok(out);
    }

    /**
     * Unified auth options — returns all available login methods for a phone or email.
     * Frontend uses this to show: "Login with OTP", "Login with Password", "Login with PIN"
     */
    @GetMapping("/auth-options")
    public ResponseEntity<Map<String, Object>> getAuthOptions(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        Map<String, Object> options = new java.util.LinkedHashMap<>();
        options.put("otp", true); // OTP always available

        if (email != null && !email.isBlank()) {
            CheckMethodResponse cm = passwordService.checkMethod(email);
            options.put("exists", cm.exists());
            options.put("password", cm.hasPassword());
            options.put("methods", cm.methods());
        }

        if (phone != null && !phone.isBlank()) {
            Map<String, Object> pinStatus = pinService.checkPinStatus(phone, null);
            options.put("exists", true);
            options.put("pin", pinStatus.get("hasPin"));
            options.put("pinLocked", pinStatus.get("pinLocked"));
            boolean hasPassword = passwordService.hasPassword(phone);
            options.put("password", hasPassword);
        }

        return ResponseEntity.ok(options);
    }

    // ── PIN-based quick login (HDFC-style) ──────────────────────────────

    @PostMapping("/pin/set")
    public ResponseEntity<Void> setPin(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SetPinRequest request) {
        pinService.setPin(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pin/change")
    public ResponseEntity<Void> changePin(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody ChangePinRequest request) {
        pinService.changePin(userId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/pin/signin")
    public ResponseEntity<AuthResponse> pinSignIn(
            @Valid @RequestBody PinSignInRequest request) {
        return ResponseEntity.ok(pinService.signInWithPin(request));
    }

    @PostMapping("/pin/reset")
    public ResponseEntity<Void> resetPin(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody SetPinRequest request) {
        pinService.resetPinAfterOtp(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * Forgot PIN — pre-auth flow. User provides phone + OTP + new PIN.
     * No login required. Verifies OTP, resets PIN, auto-logs in.
     */
    @PostMapping("/pin/forgot")
    public ResponseEntity<AuthResponse> forgotPin(
            @RequestBody Map<String, String> body) {
        String phone = body.get("phone");
        String otp = body.get("otp");
        String newPin = body.get("newPin");
        if (phone == null || otp == null || newPin == null) {
            throw new IllegalArgumentException("phone, otp, and newPin are required");
        }
        return ResponseEntity.ok(pinService.forgotPin(phone, otp, newPin));
    }

    @DeleteMapping("/pin")
    public ResponseEntity<Void> removePin(
            @RequestHeader("X-User-Id") UUID userId) {
        pinService.removePin(userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pin/check")
    public ResponseEntity<Map<String, Object>> checkPinStatus(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email) {
        return ResponseEntity.ok(pinService.checkPinStatus(phone, email));
    }

    // ── Admin impersonation (login as user for support) ────────────────
    @PostMapping("/admin/impersonate")
    public ResponseEntity<AuthResponse> impersonate(
            @RequestHeader("X-User-Id") UUID adminId,
            @RequestHeader("X-User-Role") String role,
            @RequestBody Map<String, String> body) {
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(403).build();
        }
        UUID targetUserId = UUID.fromString(body.get("targetUserId"));
        return ResponseEntity.ok(authService.impersonate(adminId, targetUserId));
    }
}
