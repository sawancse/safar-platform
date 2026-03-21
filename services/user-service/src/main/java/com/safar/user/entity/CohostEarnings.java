package com.safar.user.entity;

import com.safar.user.entity.enums.EarningsStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cohost_earnings", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CohostEarnings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID agreementId;

    @Column(nullable = false)
    private UUID bookingId;

    @Column(nullable = false)
    private Long amountPaise;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EarningsStatus status = EarningsStatus.PENDING;

    private OffsetDateTime paidAt;
}
