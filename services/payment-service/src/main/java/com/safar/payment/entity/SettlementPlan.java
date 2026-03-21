package com.safar.payment.entity;

import com.safar.payment.entity.enums.SettlementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "settlement_plans", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID bookingId;

    private UUID paymentId;

    @Column(nullable = false)
    private Long totalAmountPaise;

    @Builder.Default
    private String currency = "INR";

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SettlementStatus status = SettlementStatus.PENDING;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SettlementLine> lines = new ArrayList<>();

    @CreationTimestamp
    private OffsetDateTime createdAt;

    private OffsetDateTime completedAt;
}
