package com.safar.listing.entity;

import com.safar.listing.entity.enums.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "legal_consultations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LegalConsultation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID legalCaseId;

    @Column(nullable = false)
    private UUID advocateId;

    @Column(nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConsultationStatus status = ConsultationStatus.SCHEDULED;

    private OffsetDateTime scheduledAt;

    @Builder.Default
    private Integer durationMinutes = 30;

    @Column(length = 20)
    private String mode;

    @Column(name = "meeting_url")
    private String meetingLink;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String consultationNotes;

    @Column(columnDefinition = "TEXT")
    private String adviceGiven;

    private Long feePaise;

    private OffsetDateTime completedAt;

    @CreationTimestamp
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
