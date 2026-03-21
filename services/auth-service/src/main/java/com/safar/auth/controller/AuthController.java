package com.safar.auth.controller;

import com.safar.auth.dto.*;
import com.safar.auth.service.AuthService;
import com.safar.auth.service.OtpService;
import com.safar.auth.service.PasswordService;
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

    @PostMapping("/otp/send")
    public ResponseEntity<Void> sendOtp(
            @Valid @RequestBody SendOtpRequest request) {
        otpService.sendOtp(request.phone());
        return ResponseEntity.ok().build();
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
}
