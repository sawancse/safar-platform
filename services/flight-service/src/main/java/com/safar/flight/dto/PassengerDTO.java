package com.safar.flight.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record PassengerDTO(
        @NotBlank(message = "Title is required")
        String title,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        @NotBlank(message = "Gender is required")
        String gender,

        String nationality,

        String passportNumber,

        LocalDate passportExpiry
) {}
