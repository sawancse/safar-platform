package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/** One settlement line paid as part of a (batched) payout. */
@Entity
@Table(name = "payout_items", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID payoutId;

    @Column(nullable = false)
    private UUID settlementLineId;

    private UUID bookingId;

    @Column(nullable = false)
    private Long amountPaise;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
