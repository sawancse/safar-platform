package com.safar.payment.entity;

import com.safar.payment.entity.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "payments", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId;

    @Column(unique = true, nullable = false, length = 100)
    private String razorpayOrderId;

    @Column(unique = true, length = 100)
    private String razorpayPaymentId;

    @Column(nullable = false)
    private Long amountPaise;

    @Builder.Default
    private String currency = "INR";

    private String method;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(unique = true, length = 30)
    private String gstInvoiceNumber;

    private OffsetDateTime capturedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
