package com.safar.listing.dto;

import jakarta.validation.constraints.Pattern;

public record AgreementPartyRequest(
        String partyType,
        String fullName,
        String aadhaarNumber,
        String panNumber,
        String address,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Phone must be a 10-digit Indian mobile number")
        String phone,
        String email
) {}
