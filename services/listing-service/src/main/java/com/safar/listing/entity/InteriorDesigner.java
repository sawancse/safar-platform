package com.safar.listing.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "interior_designers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteriorDesigner {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String fullName;

    @Column(length = 200)
    private String companyName;

    @Column(length = 200)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    private Integer experienceYears;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] specializations;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private String[] portfolioUrls;

    private String profilePhotoUrl;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Builder.Default
    private Integer totalProjects = 0;

    @Column(name = "projects_completed")
    @Builder.Default
    private Integer completedProjects = 0;

    private Long consultationFeePaise;

    @Builder.Default
    private Boolean verified = false;

    @Builder.Default
    private Boolean active = true;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
