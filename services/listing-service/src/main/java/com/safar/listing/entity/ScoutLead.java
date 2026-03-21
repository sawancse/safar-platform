package com.safar.listing.entity;

import com.safar.listing.entity.enums.ScoutLeadStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "scout_leads", schema = "listings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class ScoutLead {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(precision = 9, scale = 6)
    private BigDecimal lng;

    @Column(name = "estimated_income_paise")
    private Long estimatedIncomePaise;

    @Column(name = "outreach_sent_at")
    private OffsetDateTime outreachSentAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private ScoutLeadStatus status = ScoutLeadStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
