package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "agreement_parties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgreementParty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "agreement_id", nullable = false)
    private UUID agreementRequestId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PartyType partyType;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(length = 200)
    private String fatherName;

    @Column(length = 500)
    private String address;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String email;

    @Column(length = 12)
    private String aadhaarNumber;

    @Column(length = 10)
    private String panNumber;

    private String idProofUrl;

    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ESignStatus eSignStatus = ESignStatus.PENDING;

    private String eSignRequestId;

    private OffsetDateTime signedAt;

    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
