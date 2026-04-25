package com.safar.chef.dto;

import com.safar.chef.entity.enums.VendorServiceType;

import java.util.List;

public record PartnerVendorRequest(
        VendorServiceType serviceType,
        String businessName,
        String ownerName,
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
