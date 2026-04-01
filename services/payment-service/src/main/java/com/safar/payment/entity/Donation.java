package com.safar.payment.entity;

import com.safar.payment.entity.enums.DonationStatus;
import com.safar.payment.entity.enums.DonationFrequency;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "donations", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Donation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Unique donation reference e.g. DON-2026-1001 */
    @Column(unique = true, nullable = false, length = 30)
    private String donationRef;

    /** Authenticated donor (null for anonymous) */
    private UUID donorId;

    /** Donor name for 80G receipt (optional, can be set without login) */
    private String donorName;

    /** Donor email for receipt delivery */
    private String donorEmail;

    /** Donor phone (optional) */
    private String donorPhone;

    /** Donor PAN for 80G (optional, captured post-donation) */
    @Column(length = 10)
    private String donorPan;

    @Column(nullable = false)
    private Long amountPaise;

    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DonationFrequency frequency = DonationFrequency.ONE_TIME;

    /** Razorpay order ID for one-time payments */
    @Column(unique = true, length = 100)
    private String razorpayOrderId;

    /** Razorpay payment ID after capture */
    @Column(unique = true, length = 100)
    private String razorpayPaymentId;

    /** Razorpay subscription ID for monthly donations */
    @Column(length = 100)
    private String razorpaySubscriptionId;

    /** Payment method used (upi, card, netbanking, wallet) */
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private DonationStatus status = DonationStatus.CREATED;

    /** Optional: dedicate donation to someone */
    private String dedicatedTo;

    /** Optional: dedication message */
    private String dedicationMessage;

    /** Campaign/drive this donation is part of (e.g. "diwali-2026") */
    private String campaignCode;

    /** 80G receipt number (generated after capture) */
    @Column(unique = true, length = 30)
    private String receiptNumber;

    /** Whether 80G receipt has been emailed */
    @Builder.Default
    private Boolean receiptSent = false;

    private OffsetDateTime capturedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
