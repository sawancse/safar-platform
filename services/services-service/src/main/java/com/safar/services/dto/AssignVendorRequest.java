package com.safar.services.dto;

import java.util.UUID;

public record AssignVendorRequest(
        UUID vendorId,
        Long payoutPaise,   // optional override; defaults to booking total
        String adminNotes
) {}
