package com.safar.auth.dto;

import java.util.List;

public record CheckMethodResponse(
        boolean exists,
        boolean hasPassword,
        List<String> methods
) {}
