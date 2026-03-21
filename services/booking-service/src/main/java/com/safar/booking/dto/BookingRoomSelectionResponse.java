package com.safar.booking.dto;

import java.util.UUID;

public record BookingRoomSelectionResponse(
        UUID id,
        UUID roomTypeId,
        String roomTypeName,
        Integer count,
        Long pricePerUnitPaise,
        Long totalPaise
) {}
