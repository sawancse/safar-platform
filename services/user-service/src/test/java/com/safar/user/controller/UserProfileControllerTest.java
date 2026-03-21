package com.safar.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.user.config.SecurityConfig;
import com.safar.user.dto.TasteProfileDto;
import com.safar.user.dto.UpdateTasteProfileRequest;
import com.safar.user.entity.enums.*;
import com.safar.user.security.JwtUtil;
import com.safar.user.service.HostSubscriptionService;
import com.safar.user.service.LanguageService;
import com.safar.user.service.ProfileService;
import com.safar.user.service.UserProfileService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({UserProfileController.class, HostSubscriptionController.class})
@Import(SecurityConfig.class)
class UserProfileControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean UserProfileService userProfileService;
    @MockitoBean ProfileService profileService;
    @MockitoBean HostSubscriptionService hostSubscriptionService;
    @MockitoBean LanguageService languageService;
    // Mock JwtUtil (not JwtAuthFilter) — the real filter runs and sets SecurityContext
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID USER_ID = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
    private static final String VALID_TOKEN = "valid.test.token";

    @BeforeEach
    void setupJwt() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(USER_ID.toString());
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(claims);
        when(jwtUtil.extractRole(claims)).thenReturn("GUEST");
    }

    @Test
    void getTasteProfile_notFound_returns404() throws Exception {
        when(userProfileService.getProfile(USER_ID)).thenThrow(new NoSuchElementException("not found"));

        mockMvc.perform(get("/api/v1/users/me/taste-profile")
                        .header("Authorization", "Bearer " + VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    void upsertTasteProfile_valid_returns200() throws Exception {
        TasteProfileDto dto = new TasteProfileDto(
                UUID.randomUUID(), USER_ID,
                TravelStyle.ADVENTURE, PropertyVibe.MODERN,
                List.of("wifi"), GroupType.SOLO, BudgetTier.MID,
                OffsetDateTime.now()
        );
        when(userProfileService.upsertProfile(any(), any())).thenReturn(dto);

        UpdateTasteProfileRequest req = new UpdateTasteProfileRequest(
                TravelStyle.ADVENTURE, PropertyVibe.MODERN,
                List.of("wifi"), GroupType.SOLO, BudgetTier.MID
        );

        mockMvc.perform(put("/api/v1/users/me/taste-profile")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.travelStyle").value("ADVENTURE"));
    }

    @Test
    void upsertTasteProfile_unauthenticated_returns401() throws Exception {
        // No Authorization header — no SecurityContext set — Spring Security returns 401
        mockMvc.perform(put("/api/v1/users/me/taste-profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UpdateTasteProfileRequest(null, null, null, null, null))))
                .andExpect(status().isUnauthorized());
    }
}
