package com.safar.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateKycBankRequest(
        @NotBlank String bankAccountName,
        @NotBlank String bankAccountNumber,
        @NotBlank @Pattern(regexp = "[A-Z]{4}0[A-Z0-9]{6}", message = "Invalid IFSC format") String bankIfsc,
        @NotBlank String bankName
) {}
