package com.safar.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

public record UpdateKycIdentityRequest(
        @NotBlank String fullLegalName,
        LocalDate dateOfBirth,
        @NotBlank @Pattern(regexp = "\\d{12}", message = "Aadhaar must be 12 digits") String aadhaarNumber,
        @NotBlank @Pattern(regexp = "[A-Z]{5}\\d{4}[A-Z]", message = "Invalid PAN format") String panNumber
) {}
