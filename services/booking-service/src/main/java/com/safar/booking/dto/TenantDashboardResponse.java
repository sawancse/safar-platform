package com.safar.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

public record TenantDashboardResponse(
        TenancySnapshot tenancy,
        AgreementSnapshot agreement,
        InvoiceSnapshot currentInvoice,
        long totalPaidPaise,
        long outstandingPaise,
        int openMaintenanceRequests,
        SubscriptionSnapshot subscription
) {
    public record TenancySnapshot(
            UUID id,
            String tenancyRef,
            String status,
            LocalDate moveInDate,
            LocalDate moveOutDate,
            long monthlyRentPaise,
            long securityDepositPaise,
            Integer noticePeriodDays
    ) {}

    public record AgreementSnapshot(
            String status,
            String agreementNumber,
            String pdfUrl
    ) {}

    public record InvoiceSnapshot(
            UUID id,
            String invoiceNumber,
            long grandTotalPaise,
            LocalDate dueDate,
            String status
    ) {}

    public record SubscriptionSnapshot(
            String status,
            String razorpaySubscriptionId
    ) {}
}
