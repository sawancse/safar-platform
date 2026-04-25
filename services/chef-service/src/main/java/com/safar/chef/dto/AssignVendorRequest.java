package com.safar.chef.dto;

import java.util.UUID;

public record AssignVendorRequest(
        UUID vendorId,
        Long payoutPaise,   // optional override; defaults to booking total
        String adminNotes
) {}
