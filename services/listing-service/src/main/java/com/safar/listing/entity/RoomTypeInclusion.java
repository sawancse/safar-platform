package com.safar.listing.entity;

import com.safar.listing.entity.enums.ChargeType;
import com.safar.listing.entity.enums.InclusionCategory;
import com.safar.listing.entity.enums.InclusionMode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "room_type_inclusions", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomTypeInclusion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InclusionCategory category;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "inclusion_mode", nullable = false, length = 20)
    @Builder.Default
    private InclusionMode inclusionMode = InclusionMode.INCLUDED;

    @Column(name = "charge_paise")
    @Builder.Default
    private Long chargePaise = 0L;

    @Enumerated(EnumType.STRING)
    @Column(name = "charge_type", length = 20)
    @Builder.Default
    private ChargeType chargeType = ChargeType.PER_STAY;

    @Column(name = "discount_percent")
    @Builder.Default
    private Integer discountPercent = 0;

    @Column(length = 500)
    private String terms;

    @Column(name = "is_highlight")
    @Builder.Default
    private Boolean isHighlight = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
