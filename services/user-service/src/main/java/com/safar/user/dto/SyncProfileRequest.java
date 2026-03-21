package com.safar.user.dto;

public record SyncProfileRequest(
        String name,
        String phone,
        String role
) {}
