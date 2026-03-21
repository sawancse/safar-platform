package com.safar.payment.entity;

import com.safar.payment.entity.enums.InvoiceStatus;
import com.safar.payment.entity.enums.SubscriptionTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "host_invoices", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID hostId;

    private String razorpaySubId;

    @Column(unique = true, nullable = false, length = 30)
    private String invoiceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionTier tier;

    @Column(nullable = false)
    private Long amountPaise;

    @Column(nullable = false)
    private Long gstAmountPaise;

    @Column(nullable = false)
    private Long totalPaise;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.DRAFT;

    @Column(nullable = false)
    private LocalDate billingPeriodStart;

    @Column(nullable = false)
    private LocalDate billingPeriodEnd;

    @Column(name = "pdf_s3_key")
    private String pdfS3Key;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
