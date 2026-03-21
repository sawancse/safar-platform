package com.safar.listing.entity;

import com.safar.listing.entity.enums.PolygonType;
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
@Table(name = "locality_polygons", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocalityPolygon {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(name = "osm_id", length = 50)
    private String osmId;

    @Column(name = "boundary_geo_json", columnDefinition = "TEXT")
    private String boundaryGeoJson;

    @Column(name = "centroid_lat")
    private Double centroidLat;

    @Column(name = "centroid_lng")
    private Double centroidLng;

    @Enumerated(EnumType.STRING)
    @Column(name = "polygon_type", nullable = false, length = 30)
    @Builder.Default
    private PolygonType polygonType = PolygonType.NEIGHBORHOOD;

    @Column(name = "listing_count", nullable = false)
    @Builder.Default
    private int listingCount = 0;

    @Column(name = "area_km2")
    private Double areaKm2;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "last_updated_from_osm")
    private OffsetDateTime lastUpdatedFromOsm;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
