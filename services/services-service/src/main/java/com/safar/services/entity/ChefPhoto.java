package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_photos", schema = "chefs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefPhoto {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    private String caption;

    @Column(name = "photo_type", length = 30)
    @Builder.Default
    private String photoType = "FOOD";

    /** IMAGE or VIDEO */
    @Column(name = "media_type", length = 10)
    @Builder.Default
    private String mediaType = "IMAGE";

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
