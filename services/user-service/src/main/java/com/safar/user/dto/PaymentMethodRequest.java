package com.safar.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PaymentMethodRequest(
        @NotBlank @Pattern(regexp = "^(UPI|CREDIT_CARD|DEBIT_CARD|NET_BANKING)$",
                message = "Type must be UPI, CREDIT_CARD, DEBIT_CARD, or NET_BANKING")
        String type,

        @Size(max = 100) String label,
        Boolean isDefault,

        // UPI
        @Size(max = 80) String upiId,

        // Card
        @Size(max = 4) String cardLast4,
        @Size(max = 20) String cardNetwork,
        @Size(max = 100) String cardHolder,
        @Size(max = 7) String cardExpiry,

        // Net Banking
        @Size(max = 100) String bankName,
        @Size(max = 4) String bankAccountLast4
) {}
