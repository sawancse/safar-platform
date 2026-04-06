package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "materials_catalog")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaterialCatalog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "material_name", nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String brand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MaterialCategory category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String sku;

    @Column(name = "unit_price_paise")
    private Long pricePerUnitPaise;

    @Column(length = 50)
    private String unit;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] photos;

    @Column(length = 200)
    private String color;

    @Column(length = 200)
    private String finish;

    @Column(length = 200)
    private String dimensions;

    @Column(length = 100)
    private String warranty;

    @Builder.Default
    private Boolean active = true;

    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
