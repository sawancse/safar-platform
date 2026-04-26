package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "cake_attributes", schema = "services")
@DiscriminatorValue("CAKE_DESIGNER")
@PrimaryKeyJoinColumn(name = "service_listing_id")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class CakeAttributes extends ServiceListing {

    @Column(name = "bakery_type", length = 30)
    private String bakeryType;                  // HOME_BAKER, COMMERCIAL, CLOUD_KITCHEN

    @Column(name = "oven_capacity_kg_per_day")
    private Integer ovenCapacityKgPerDay;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "flavours_offered", columnDefinition = "varchar(40)[]")
    private List<String> flavoursOffered;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "design_styles", columnDefinition = "varchar(40)[]")
    private List<String> designStyles;

    @Column(name = "max_tier_count")
    private Integer maxTierCount;

    @Column(name = "eggless_capable")
    private Boolean egglessCapable;

    @Column(name = "vegan_capable")
    private Boolean veganCapable;

    @Column(name = "delivery_mode", length = 20)
    private String deliveryMode;                // SELF, PARTNER, PICKUP_ONLY
}
