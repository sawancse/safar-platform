package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nomad_prime_memberships", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NomadPrimeMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID guestId;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    @Builder.Default
    private Integer discountPct = 15;

    @Column(nullable = false)
    @Builder.Default
    private Integer monthlyBonusMiles = 500;

    @Column(nullable = false)
    @Builder.Default
    private Long insuranceCoverPaise = 50000000L;

    @Column(nullable = false)
    @Builder.Default
    private OffsetDateTime startedAt = OffsetDateTime.now();

    @Column(nullable = false)
    private LocalDate nextRenewalDate;

    private OffsetDateTime cancelledAt;
}
