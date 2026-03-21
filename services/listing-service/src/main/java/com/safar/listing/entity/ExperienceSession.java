package com.safar.listing.entity;

import com.safar.listing.entity.enums.SessionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "experience_sessions", schema = "listings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExperienceSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "experience_id", nullable = false)
    private UUID experienceId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "available_spots", nullable = false)
    private Integer availableSpots;

    @Column(name = "booked_spots", nullable = false)
    @Builder.Default
    private Integer bookedSpots = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private SessionStatus status = SessionStatus.OPEN;
}
