package com.safar.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ExpenseRequest(
        UUID listingId,
        @NotBlank String category,
        @NotNull Long amountPaise,
        Long gstPaise,
        String description,
        @NotNull LocalDate expenseDate
) {}
