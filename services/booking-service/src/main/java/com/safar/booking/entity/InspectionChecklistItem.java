package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "inspection_checklist_items", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InspectionChecklistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private TenancySettlement settlement;

    @Column(nullable = false, length = 50)
    private String area;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "condition", nullable = false, length = 20)
    private String condition;

    @Column(name = "damage_description", length = 500)
    private String damageDescription;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Column(name = "deduction_paise", nullable = false)
    @Builder.Default
    private long deductionPaise = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
