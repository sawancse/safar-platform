package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "skill_swaps", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SkillSwap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID posterId;

    @Column(nullable = false)
    private String offering;

    @Column(nullable = false)
    private String seeking;

    private String city;

    @Column(nullable = false)
    @Builder.Default
    private String status = "OPEN";

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
