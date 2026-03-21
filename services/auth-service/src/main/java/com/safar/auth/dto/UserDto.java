package com.safar.auth.dto;

import java.util.UUID;

public record UserDto(
        UUID id,
        String phone,
        String email,
        String name,
        String role,
        String kycStatus,
        String avatarUrl,
        String language,
        boolean hasPassword
) {}
