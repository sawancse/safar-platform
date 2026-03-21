package com.safar.booking.dto;

import java.util.UUID;

public record BookingGuestResponse(
        UUID id,
        String fullName,
        String email,
        String phone,
        Integer age,
        String idType,
        String idNumber,
        String roomAssignment,
        Boolean isPrimary
) {}
