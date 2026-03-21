package com.safar.booking.entity;

import com.safar.booking.entity.enums.CleaningJobStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cleaning_jobs", schema = "bookings")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CleaningJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID listingId;

    private UUID bookingId;

    private UUID cleanerId;

    @Column(nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(nullable = false, precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal estimatedHours = new BigDecimal("2.0");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CleaningJobStatus status = CleaningJobStatus.UNASSIGNED;

    private OffsetDateTime completedAt;

    private Long amountPaise;

    private String notes;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
