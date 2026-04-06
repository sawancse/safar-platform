package com.safar.listing.dto;

public record AgreementPartyRequest(
        String partyType,
        String fullName,
        String aadhaarNumber,
        String panNumber,
        String address,
        String phone,
        String email
) {}
