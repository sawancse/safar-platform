package com.safar.booking.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateBookingRequest(
        @NotNull UUID listingId,
        @NotNull LocalDateTime checkIn,
        @NotNull LocalDateTime checkOut,
        @NotNull @Min(1) Integer guestsCount,
        Integer adultsCount,
        Integer childrenCount,
        Integer infantsCount,
        Integer petsCount,
        Integer roomsCount,
        @NotBlank String guestFirstName,
        @NotBlank String guestLastName,
        @NotBlank String guestEmail,
        @NotBlank String guestPhone,
        String bookingFor,
        Boolean travelForWork,
        Boolean airportShuttle,
        String specialRequests,
        String arrivalTime,
        UUID roomTypeId,
        String bookingType,
        UUID organizationId,
        UUID caseWorkerId,
        Long monthlyRatePaise,
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
        // PG/Hotel booking fields
        Integer noticePeriodDays,
        Long securityDepositPaise,
        // Hourly bookings
        Integer hours, // for HOUR pricing unit
        // Non-refundable & Pay-at-Property
        Boolean nonRefundable,
        String paymentMode, // PREPAID, PAY_AT_PROPERTY, PARTIAL_PREPAID
        // Inclusions & Perks — list of inclusion IDs the guest selected (PAID_ADDON items)
        List<UUID> selectedInclusionIds,
        // Multi-room-type selections: [{roomTypeId, count}]
        List<RoomSelection> roomSelections,
        // Guest list
        List<GuestInfo> guests
) {
    public record RoomSelection(UUID roomTypeId, int count) {}
    public record GuestInfo(
            String fullName, String email, String phone,
            Integer age, String idType, String idNumber,
            String roomAssignment, Boolean isPrimary
    ) {}
}
