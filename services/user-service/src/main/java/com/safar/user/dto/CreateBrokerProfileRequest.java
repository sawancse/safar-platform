package com.safar.user.dto;

import java.util.List;

public record CreateBrokerProfileRequest(
        String companyName,
        String reraAgentId,
        List<String> operatingCities,
        String specialization,
        Integer experienceYears,
        String bio,
        String website,
        String officeAddress,
        String officeCity,
        String officeState,
        String officePincode
) {}
