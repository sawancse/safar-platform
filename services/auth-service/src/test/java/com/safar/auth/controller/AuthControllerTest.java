package com.safar.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.auth.dto.*;
import com.safar.auth.service.AuthService;
import com.safar.auth.service.OtpService;
import com.safar.auth.config.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean OtpService  otpService;
    @MockitoBean AuthService authService;

    private static final String VALID_PHONE = "+919876543210";

    @Test
    void sendOtp_validPhone_returns200() throws Exception {
        doNothing().when(otpService).sendOtp(VALID_PHONE);

        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendOtpRequest(VALID_PHONE))))
                .andExpect(status().isOk());

        verify(otpService).sendOtp(VALID_PHONE);
    }

    @Test
    void sendOtp_invalidPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SendOtpRequest("12345"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(otpService);
    }

    @Test
    void sendOtp_blankPhone_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void verifyOtp_validRequest_returns200WithTokens() throws Exception {
        UserDto userDto = new UserDto(UUID.randomUUID(), VALID_PHONE,
                null, "Rahul", "GUEST", "PENDING", null, "en", false);
        AuthResponse response = new AuthResponse(
                "access_token", "refresh_token", 900L, userDto);

        when(authService.verifyOtpAndLogin(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest(VALID_PHONE, "123456", "Rahul", null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access_token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh_token"))
                .andExpect(jsonPath("$.user.phone").value(VALID_PHONE));
    }

    @Test
    void verifyOtp_wrongOtp_returns401() throws Exception {
        when(authService.verifyOtpAndLogin(any()))
                .thenThrow(new IllegalArgumentException("Invalid or expired OTP"));

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest(VALID_PHONE, "000000", "Rahul", null))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void verifyOtp_invalidOtpLength_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new VerifyOtpRequest(VALID_PHONE, "123", "Rahul", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refresh_validToken_returns200() throws Exception {
        UserDto userDto = new UserDto(UUID.randomUUID(), VALID_PHONE,
                null, "Rahul", "GUEST", "PENDING", null, "en", false);
        AuthResponse response = new AuthResponse(
                "new_access_token", "new_refresh_token", 900L, userDto);

        when(authService.refreshTokens("valid_refresh")).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("valid_refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new_access_token"));
    }

    @Test
    void logout_validToken_returns204() throws Exception {
        doNothing().when(authService).logout("valid_refresh");

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RefreshTokenRequest("valid_refresh"))))
                .andExpect(status().isNoContent());

        verify(authService).logout("valid_refresh");
    }
}
