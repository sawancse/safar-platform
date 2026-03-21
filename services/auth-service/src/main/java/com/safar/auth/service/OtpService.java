package com.safar.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;

    @Value("${otp.expiry-minutes:10}")
    private int otpExpiryMinutes;

    @Value("${spring.mail.username:sawank.sit@gmail.com}")
    private String fromEmail;

    @Value("${otp.dev-mode:true}")
    private boolean devMode;

    private static final String OTP_PREFIX    = "otp:";
    private static final String RATE_PREFIX   = "otp:rate:";
    private static final String DEV_OTP       = "123456";
    private static final int    MAX_ATTEMPTS  = 3;

    public void sendOtp(String phone) {
        checkRateLimit(phone);

        String otp = generateOtp();
        redis.opsForValue().set(
                OTP_PREFIX + phone,
                otp,
                Duration.ofMinutes(otpExpiryMinutes)
        );

        // In production: send via SMS provider (Twilio / AWS SNS)
        // SMS template for production (Android SMS Retriever compatible):
        // "<#> Your Safar verification code is: {otp}. Valid for {otpExpiryMinutes} minutes.\n{APP_HASH}"
        // The APP_HASH is generated from your app's signing certificate
        // In dev: log the OTP
        log.info("OTP for {}: {} (dev mode — remove in prod)", phone, otp);
    }

    public void sendEmailOtp(String email) {
        checkRateLimit(email);

        String otp = generateOtp();
        redis.opsForValue().set(
                OTP_PREFIX + email,
                otp,
                Duration.ofMinutes(otpExpiryMinutes)
        );

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(fromEmail);
            msg.setTo(email);
            msg.setSubject("Your Safar OTP: " + otp);
            msg.setText("Your Safar verification code is: " + otp
                    + "\n\nThis code expires in " + otpExpiryMinutes + " minutes."
                    + "\n\nIf you didn't request this, please ignore this email."
                    + "\n\n— Safar Team");
            mailSender.send(msg);
            log.info("Email OTP sent to {}", email);
        } catch (Exception e) {
            log.error("Failed to send email OTP to {}: {}", email, e.getMessage(), e);
            // Still store OTP in Redis so dev can verify with logged OTP
            log.info("Email OTP for {} (fallback — email delivery failed): {}", email, otp);
            // Don't throw — OTP is still valid via console in dev mode
            // In production, uncomment the throw below:
            // throw new IllegalStateException("Failed to send OTP email. Please try again.");
        }
    }

    public void sendWhatsAppOtp(String phone) {
        checkRateLimit(phone);

        String otp = generateOtp();
        redis.opsForValue().set(
                OTP_PREFIX + phone,
                otp,
                Duration.ofMinutes(otpExpiryMinutes)
        );

        // In production: send via WhatsApp Business API (Meta Cloud API)
        // Requires WhatsApp Business Account + approved message template
        // WhatsApp Business API template: "Your Safar verification code is: {{1}}. Valid for {{2}} minutes."
        log.info("WhatsApp OTP for {}: {} (dev mode — integrate WhatsApp Business API in prod)", phone, otp);
    }

    public boolean verifyOtp(String identifier, String submittedOtp) {
        // Dev mode: always accept 123456
        if (devMode && DEV_OTP.equals(submittedOtp)) {
            redis.delete(OTP_PREFIX + identifier);
            redis.delete(RATE_PREFIX + identifier);
            log.info("Dev mode: accepted OTP {} for {}", DEV_OTP, identifier);
            return true;
        }
        String stored = redis.opsForValue().get(OTP_PREFIX + identifier);
        if (stored != null && stored.equals(submittedOtp)) {
            redis.delete(OTP_PREFIX + identifier);
            redis.delete(RATE_PREFIX + identifier);
            return true;
        }
        return false;
    }

    private void checkRateLimit(String identifier) {
        String key = RATE_PREFIX + identifier;
        Long count = redis.opsForValue().increment(key);
        if (count == 1) {
            redis.expire(key, Duration.ofHours(1));
        }
        if (count > MAX_ATTEMPTS) {
            throw new IllegalStateException(
                    "Too many OTP requests. Try again in 1 hour.");
        }
    }

    private String generateOtp() {
        String otp = String.format("%06d", new SecureRandom().nextInt(999999));
        log.info("Generated OTP: {}", otp);
        return otp;
    }
}
