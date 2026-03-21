package com.safar.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock StringRedisTemplate redis;
    @Mock ValueOperations<String, String> valueOps;

    @InjectMocks OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "otpExpiryMinutes", 10);
        when(redis.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void sendOtp_firstRequest_storesOtpInRedis() {
        when(valueOps.increment("otp:rate:+919876543210")).thenReturn(1L);

        otpService.sendOtp("+919876543210");

        verify(valueOps).set(
                eq("otp:+919876543210"),
                anyString(),
                eq(Duration.ofMinutes(10))
        );
    }

    @Test
    void sendOtp_exceeds3Attempts_throwsException() {
        when(valueOps.increment("otp:rate:+919876543210")).thenReturn(4L);

        assertThatThrownBy(() -> otpService.sendOtp("+919876543210"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Too many OTP requests");
    }

    @Test
    void verifyOtp_correctCode_returnsTrue() {
        when(valueOps.get("otp:+919876543210")).thenReturn("123456");

        boolean result = otpService.verifyOtp("+919876543210", "123456");

        assertThat(result).isTrue();
        verify(redis).delete("otp:+919876543210");
    }

    @Test
    void verifyOtp_wrongCode_returnsFalse() {
        when(valueOps.get("otp:+919876543210")).thenReturn("123456");

        boolean result = otpService.verifyOtp("+919876543210", "999999");

        assertThat(result).isFalse();
        verify(redis, never()).delete(anyString());
    }

    @Test
    void verifyOtp_expiredOtp_returnsFalse() {
        when(valueOps.get("otp:+919876543210")).thenReturn(null);

        boolean result = otpService.verifyOtp("+919876543210", "123456");

        assertThat(result).isFalse();
    }

    @Test
    void verifyOtp_success_deletesOtpAndRateKey() {
        when(valueOps.get("otp:+919876543210")).thenReturn("123456");

        otpService.verifyOtp("+919876543210", "123456");

        verify(redis).delete("otp:+919876543210");
        verify(redis).delete("otp:rate:+919876543210");
    }
}
