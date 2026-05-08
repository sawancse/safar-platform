package com.safar.services.dto;

import com.safar.services.entity.enums.VendorServiceType;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record PartnerVendorRequest(
        VendorServiceType serviceType,
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
        List<String> serviceCities,
        Integer serviceRadiusKm,
        String portfolioJson,
        String pricingOverrideJson,
        String kycStatus,
        String kycNotes,
        String notes,
        Boolean active
) {}
