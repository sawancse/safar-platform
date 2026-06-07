package com.safar.listing.dto;

/**
 * Admin edit of an agreement's terms (NoBroker-style): dates, rent, deposit,
 * parties, clauses, and the stamp-duty basis. All fields optional — only the
 * non-null ones are applied. Stamp duty/registration are recomputed from the
 * resolved basis after applying the changes.
 */
public record AdminUpdateAgreementRequest(
        String agreementType,
        String state,
        String city,
        String agreementDate,        // ISO yyyy-MM-dd
        String startDate,            // ISO yyyy-MM-dd
        String endDate,              // ISO yyyy-MM-dd
        Long monthlyRentPaise,
        Long securityDepositPaise,
        Long saleConsiderationPaise,
        Long propertyValuePaise,     // explicit stamp-duty basis; falls back to sale/rent
        String packageType,
        String clausesJson,
        String partyDetailsJson,
        String notes
) {}
