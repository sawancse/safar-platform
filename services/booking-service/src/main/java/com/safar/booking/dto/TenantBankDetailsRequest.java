package com.safar.booking.dto;

public record TenantBankDetailsRequest(
        String bankAccount,
        String ifsc,
        String upiId
) {}
