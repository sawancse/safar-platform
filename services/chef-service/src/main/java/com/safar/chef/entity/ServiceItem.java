package com.safar.chef.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "service_items", schema = "services")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServiceItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "service_listing_id", nullable = false)
    private UUID serviceListingId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "hero_photo_url", columnDefinition = "TEXT")
    private String heroPhotoUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> photos;

    @Column(name = "description_md", columnDefinition = "TEXT")
    private String descriptionMd;

    @Column(name = "base_price_paise", nullable = false)
    private Long basePricePaise;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "options_json", columnDefinition = "jsonb")
    private String optionsJson;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "occasion_tags", columnDefinition = "varchar(40)[]")
    private List<String> occasionTags;

    @Column(name = "lead_time_hours")
    private Integer leadTimeHours;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";           // ACTIVE | PAUSED

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
