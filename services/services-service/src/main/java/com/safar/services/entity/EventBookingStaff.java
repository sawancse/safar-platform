package com.safar.services.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "event_booking_staff", schema = "chefs",
       uniqueConstraints = @UniqueConstraint(name = "uk_event_booking_staff",
                                             columnNames = {"booking_id", "staff_id"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventBookingStaff {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "staff_id", nullable = false)
    private UUID staffId;

    @Column(nullable = false, length = 30)
    private String role;

    @Column(name = "rate_paise", nullable = false)
    private Long ratePaise;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "check_in_at")
    private OffsetDateTime checkInAt;

    @Column(name = "check_in_otp", length = 6)
    private String checkInOtp;

    private Short rating;

    @Column(name = "rated_at")
    private OffsetDateTime ratedAt;

    @Column(name = "rating_comment", columnDefinition = "TEXT")
    private String ratingComment;

    @Column(name = "no_show", nullable = false)
    @Builder.Default
    private Boolean noShow = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
