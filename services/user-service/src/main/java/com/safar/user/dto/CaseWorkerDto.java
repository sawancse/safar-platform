package com.safar.user.dto;

import java.util.UUID;

public record CaseWorkerDto(
        UUID id,
        UUID userId,
        UUID organizationId,
        String role,
        Boolean active
) {}
