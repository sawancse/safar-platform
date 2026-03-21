package com.safar.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record SendWhatsAppOtpRequest(@NotBlank String phone) {}
