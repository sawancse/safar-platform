package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fx_locks", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxLock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId;

    @Column(nullable = false, length = 3)
    private String sourceCurrency;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String targetCurrency = "INR";

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal lockedRate;

    @Column(nullable = false)
    private Long sourceAmount;

    @Column(nullable = false)
    private Long targetAmountPaise;

    @Column(nullable = false)
    private OffsetDateTime lockedAt;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;

    @Builder.Default
    private Boolean used = false;
}
