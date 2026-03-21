package com.safar.user.entity;

import com.safar.user.entity.enums.AgreementStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cohost_agreements", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CohostAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID listingId;

    @Column(nullable = false)
    private UUID hostId;

    @Column(nullable = false)
    private UUID cohostId;

    @Column(nullable = false)
    private Integer feePct;

    @Column(nullable = false)
    @Builder.Default
    private String services = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.PENDING;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
