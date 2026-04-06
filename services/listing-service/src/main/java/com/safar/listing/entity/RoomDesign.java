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
@Table(name = "room_designs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomDesign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "project_id", nullable = false)
    private UUID interiorProjectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InteriorRoomType roomType;

    @Column(length = 200)
    private String roomName;

    private Integer areaSqft;

    @Enumerated(EnumType.STRING)
    private DesignStyle designStyle;

    @Column(name = "design_2d_url")
    private String design2dUrl;

    @Column(name = "design_3d_url")
    private String design3dUrl;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] photos;

    @Column(columnDefinition = "TEXT")
    private String specifications;

    private Long estimatedCostPaise;

    @Builder.Default
    private Boolean approved = false;

    private OffsetDateTime approvedAt;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
