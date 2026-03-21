package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "gst_invoices", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GstInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 30)
    private String invoiceNumber;

    @Column(nullable = false)
    private UUID hostId;

    private UUID bookingId;

    @Column(length = 200)
    private String guestName;

    @Column(length = 15)
    private String guestGstin;

    @Column(nullable = false)
    private Long taxableAmount;

    @Builder.Default
    private Long cgstAmount = 0L;

    @Builder.Default
    private Long sgstAmount = 0L;

    @Builder.Default
    private Long igstAmount = 0L;

    @Column(nullable = false)
    private Long totalAmount;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    @Column(length = 500)
    private String pdfUrl;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
