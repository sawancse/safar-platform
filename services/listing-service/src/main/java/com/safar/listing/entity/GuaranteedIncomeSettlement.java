package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "guaranteed_income_settlements", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GuaranteedIncomeSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "contract_id", nullable = false)
    private UUID contractId;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(name = "actual_revenue", nullable = false)
    private Long actualRevenue;

    @Column(name = "guarantee_amount", nullable = false)
    private Long guaranteeAmount;

    @Column(nullable = false)
    @Builder.Default
    private Long shortfall = 0L;

    @Column(name = "safar_topped_up", nullable = false)
    @Builder.Default
    private Long safarToppedUp = 0L;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDING";

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;
}
