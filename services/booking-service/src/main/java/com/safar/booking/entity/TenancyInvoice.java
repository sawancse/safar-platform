package com.safar.booking.entity;

import com.safar.booking.entity.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenancy_invoices", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenancyInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Column(name = "billing_month", nullable = false)
    private int billingMonth;

    @Column(name = "billing_year", nullable = false)
    private int billingYear;

    @Column(name = "rent_paise", nullable = false)
    private long rentPaise;

    @Column(name = "packages_paise", nullable = false)
    @Builder.Default
    private long packagesPaise = 0;

    @Column(name = "electricity_paise", nullable = false)
    @Builder.Default
    private long electricityPaise = 0;

    @Column(name = "total_paise", nullable = false)
    private long totalPaise;

    @Column(name = "gst_paise", nullable = false)
    @Builder.Default
    private long gstPaise = 0;

    @Column(name = "grand_total_paise", nullable = false)
    private long grandTotalPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.GENERATED;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_date")
    private LocalDate paidDate;

    @Column(name = "razorpay_payment_id", length = 100)
    private String razorpayPaymentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
