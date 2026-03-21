package com.safar.user.dto;

import java.util.List;

public record KycAdminDetailDto(
        HostKycDto kyc,
        int trustScore,
        String trustBadge,
        String verificationLevel,
        int fraudRiskScore,
        List<String> fraudFlags,
        String hostType,
        String residentStatus
) {}
