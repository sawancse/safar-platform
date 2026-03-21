package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "fx_rates", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FxRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String baseCurrency = "USD";

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String targetCurrency = "INR";

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal rate;

    @Column(nullable = false, precision = 16, scale = 6)
    private BigDecimal marginRate;

    @Column(nullable = false)
    private OffsetDateTime fetchedAt;

    @Column(nullable = false)
    private OffsetDateTime expiresAt;
}
