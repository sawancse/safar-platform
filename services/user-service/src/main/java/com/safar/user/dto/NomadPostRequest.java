package com.safar.user.dto;

public record NomadPostRequest(
        String city,
        String category,
        String title,
        String body,
        String tags
) {}
