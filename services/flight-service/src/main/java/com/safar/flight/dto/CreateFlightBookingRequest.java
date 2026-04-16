package com.safar.flight.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateFlightBookingRequest(
        @NotBlank(message = "Offer ID is required")
        String offerId,

        @NotEmpty(message = "At least one passenger is required")
        @Valid
        List<PassengerDTO> passengers,

        @NotBlank(message = "Contact email is required")
        @Email(message = "Invalid email format")
        String contactEmail,

        @NotBlank(message = "Contact phone is required")
        String contactPhone
) {}
