package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "host_tax_profiles", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HostTaxProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID hostId;

    @Column(length = 15)
    private String gstin;

    @Column(nullable = false, length = 10)
    private String pan;

    @Column(length = 200)
    private String businessName;

    @Column(columnDefinition = "TEXT")
    private String registeredAddress;

    @Column(length = 2)
    private String stateCode;

    @Builder.Default
    private Boolean compositionScheme = false;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
