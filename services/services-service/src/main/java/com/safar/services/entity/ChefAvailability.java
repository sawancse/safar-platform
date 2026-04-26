package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "chef_availability", schema = "chefs",
       uniqueConstraints = @UniqueConstraint(columnNames = {"chef_id", "blocked_date"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChefAvailability {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chef_id", nullable = false)
    private UUID chefId;

    @Column(name = "blocked_date", nullable = false)
    private LocalDate blockedDate;

    private String reason;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
