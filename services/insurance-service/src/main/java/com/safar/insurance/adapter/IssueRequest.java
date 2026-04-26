package com.safar.insurance.adapter;

import java.time.LocalDate;
import java.util.List;

/**
 * Adapter-facing policy issue request. Provider-specific KYC fields
 * (PAN, passport, Aadhaar) are passed in-band as the adapter needs.
 */
public record IssueRequest(
        String providerQuoteToken,           // from QuoteResult
        List<Traveller> travellers,
        String contactEmail,
        String contactPhone
) {
    public record Traveller(
            String firstName,
            String lastName,
            LocalDate dateOfBirth,
            String gender,                   // "M" / "F" / "X"
            String nationality,              // ISO-3166-1 alpha-2
            String passportNumber,           // intl trips only
            LocalDate passportExpiry
    ) {}
}
