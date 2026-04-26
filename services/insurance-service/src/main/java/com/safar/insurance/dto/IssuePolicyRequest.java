package com.safar.insurance.dto;

import com.safar.insurance.entity.enums.CoverageType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record IssuePolicyRequest(
        @NotBlank
        String quoteId,                  // provider-prefixed: "ACKO:abc"

        String tripOriginCode,
        String tripDestinationCode,
        String tripOriginCountry,
        String tripDestinationCountry,

        @NotNull LocalDate tripStartDate,
        @NotNull LocalDate tripEndDate,
        @NotNull CoverageType coverageType,

        @NotEmpty
        @Valid
        List<TravellerDTO> travellers,

        @NotBlank
        @Email
        String contactEmail,

        @NotBlank
        String contactPhone
) {
    public record TravellerDTO(
            @NotBlank String firstName,
            @NotBlank String lastName,
            @NotNull LocalDate dateOfBirth,
            String gender,
            String nationality,
            String passportNumber,
            LocalDate passportExpiry
    ) {}
}
