package com.safar.supply.dto;

import java.util.List;

public record SupplierRequest(
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
