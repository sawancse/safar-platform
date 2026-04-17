package com.safar.booking.dto;

import com.safar.booking.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        String bookingRef,
        UUID guestId,
        UUID hostId,
        UUID listingId,
        String listingTitle,
        String listingPhotoUrl,
        String listingCity,
        String listingType,
        LocalDateTime checkIn,
        LocalDateTime checkOut,
        Integer guestsCount,
        Integer adultsCount,
        Integer childrenCount,
        Integer infantsCount,
        Integer petsCount,
        Integer roomsCount,
        Integer nights,
        BookingStatus status,
        Long baseAmountPaise,
        Long insuranceAmountPaise,
        Long gstAmountPaise,
        Long totalAmountPaise,
        Long hostPayoutPaise,
        String guestFirstName,
        String guestLastName,
        String guestEmail,
        String guestPhone,
        String bookingFor,
        Boolean travelForWork,
        Boolean airportShuttle,
        String specialRequests,
        String arrivalTime,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime checkedInAt,
        OffsetDateTime completedAt,
        String bookingType,
        UUID organizationId,
        UUID caseWorkerId,
        Long monthlyRatePaise,
        // Room type fields
        UUID roomTypeId,
        String roomTypeName,
        // Medical booking fields
        String procedureName,
        String hospitalName,
        UUID hospitalId,
        String specialty,
        LocalDate procedureDate,
        Integer hospitalDays,
        Integer recoveryDays,
        Long treatmentCostPaise,
        String patientNotes,
        // Commission model fields
        Long hostEarningsPaise,
        Long platformFeePaise,
        Long cleaningFeePaise,
        BigDecimal commissionRate,
        // Review tracking fields
        Boolean hasReview,
        Integer reviewRating,
        OffsetDateTime reviewedAt,
        // PG/Hotel booking fields
        Integer noticePeriodDays,
        Long securityDepositPaise,
        String securityDepositStatus,
        // Inclusions & Perks
        Long inclusionsTotalPaise,
        List<BookingInclusionResponse> inclusions,
        // Multi-room-type selections
        List<BookingRoomSelectionResponse> roomSelections,
        // Guest list
        List<BookingGuestResponse> guests,
        // Pricing unit
        String pricingUnit,
        // Payment mode: PREPAID / PAY_AT_PROPERTY / PARTIAL_PREPAID
        String paymentMode
) {}
