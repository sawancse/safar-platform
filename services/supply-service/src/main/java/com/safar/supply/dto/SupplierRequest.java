package com.safar.supply.dto;

import jakarta.validation.constraints.Pattern;

import java.util.List;

public record SupplierRequest(
        String businessName,
        String ownerName,
        @Pattern(regexp = "^(\\+?91)?[6-9]\\d{9}$",
                message = "Phone must be a 10-digit Indian mobile number")
        String phone,
        String email,
        String whatsapp,
        String gst,
        String pan,
        String bankAccount,
        String bankIfsc,
        String bankHolder,
        String address,
        List<String> categories,
        List<String> serviceCities,
        Integer leadTimeDays,
        String paymentTerms,
        Long creditLimitPaise,
        String kycStatus,
        String kycNotes,
        String notes,
        Boolean active
) {}
