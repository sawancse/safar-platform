package com.safar.listing.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.listing.config.SecurityConfig;
import com.safar.listing.dto.CreateListingRequest;
import com.safar.listing.dto.ListingResponse;
import com.safar.listing.entity.enums.*;
import com.safar.listing.security.JwtUtil;
import com.safar.listing.service.AvailabilityService;
import com.safar.listing.service.InvestmentSignalService;
import com.safar.listing.service.ListingService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ListingController.class)
@Import(SecurityConfig.class)
class ListingControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockitoBean ListingService listingService;
    @MockitoBean AvailabilityService availabilityService;
    @MockitoBean InvestmentSignalService investmentSignalService;
    @MockitoBean com.safar.listing.service.CommunityVerificationService communityVerificationService;
    @MockitoBean com.safar.listing.repository.ListingMediaRepository listingMediaRepository;
    @MockitoBean com.safar.listing.service.S3StorageService s3StorageService;
    // Mock JwtUtil (not JwtAuthFilter) — the real filter runs and sets SecurityContext
    @MockitoBean JwtUtil jwtUtil;

    private static final UUID HOST_ID = UUID.fromString("660e8400-e29b-41d4-a716-446655440000");
    private static final UUID LISTING_ID = UUID.fromString("770e8400-e29b-41d4-a716-446655440000");
    private static final String VALID_TOKEN = "valid.test.token";

    @BeforeEach
    void setupJwt() {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn(HOST_ID.toString());
        when(jwtUtil.validateToken(VALID_TOKEN)).thenReturn(claims);
        when(jwtUtil.extractRole(claims)).thenReturn("HOST");
    }

    private ListingResponse sampleResponse() {
        // Builder: set only the fields under test; the rest default to null/0.
        // New record fields no longer break this construction.
        return ListingResponse.builder()
                .id(LISTING_ID).hostId(HOST_ID)
                .title("Test Villa").description("A lovely villa")
                .type(ListingType.HOME)
                .addressLine1("123 MG Road").city("Mumbai").state("Maharashtra").pincode("400001")
                .lat(new BigDecimal("19.076090")).lng(new BigDecimal("72.877426"))
                .maxGuests(4).bedrooms(2).bathrooms(2).amenities(List.of("wifi"))
                .basePricePaise(500000L).pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).status(ListingStatus.DRAFT).gstApplicable(true)
                .petFriendly(false).maxPets(0).avgRating(0.0).reviewCount(0)
                .createdAt(OffsetDateTime.now()).updatedAt(OffsetDateTime.now())
                .build();
    }

    @Test
    void createListing_valid_returns201() throws Exception {
        when(listingService.createListing(any(), any())).thenReturn(sampleResponse());

        CreateListingRequest req = CreateListingRequest.builder()
                .title("Test Villa").description("A lovely villa").type(ListingType.HOME)
                .addressLine1("123 MG Road").city("Mumbai").state("Maharashtra").pincode("400001")
                .lat(new BigDecimal("19.076090")).lng(new BigDecimal("72.877426"))
                .maxGuests(4).bedrooms(2).bathrooms(2)
                .basePricePaise(500000L).pricingUnit(PricingUnit.NIGHT).minBookingHours(1)
                .instantBook(false).gstApplicable(true)
                .build();

        mockMvc.perform(post("/api/v1/listings")
                        .header("Authorization", "Bearer " + VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Test Villa"))
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getListing_noAuth_returns200() throws Exception {
        when(listingService.getListing(LISTING_ID)).thenReturn(sampleResponse());

        mockMvc.perform(get("/api/v1/listings/" + LISTING_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(LISTING_ID.toString()));
    }

    @Test
    void searchListings_noAuth_returns200() throws Exception {
        when(listingService.searchListings(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse())));

        mockMvc.perform(get("/api/v1/listings"))
                .andExpect(status().isOk());
    }

    @Test
    void createListing_unauthenticated_returns401() throws Exception {
        // No Authorization header — no SecurityContext — Spring Security returns 401
        mockMvc.perform(post("/api/v1/listings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }
}
