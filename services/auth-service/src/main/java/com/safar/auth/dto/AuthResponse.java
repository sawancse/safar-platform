package com.safar.auth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long accessTokenExpiresIn,
        UserDto user
) {}
