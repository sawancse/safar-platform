package com.safar.booking.entity;

import com.safar.booking.entity.enums.MaintenanceCategory;
import com.safar.booking.entity.enums.MaintenancePriority;
import com.safar.booking.entity.enums.MaintenanceStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "maintenance_requests", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id")
    private UUID tenancyId;

    @Column(name = "booking_id")
    private UUID bookingId;

    @Column(name = "guest_id")
    private UUID guestId;

    @Column(name = "property_type", length = 20)
    private String propertyType;

    @Column(name = "listing_id")
    private UUID listingId;

    @Column(name = "request_number", unique = true, length = 20)
    private String requestNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MaintenanceCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "photo_urls", columnDefinition = "TEXT")
    private String photoUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MaintenancePriority priority = MaintenancePriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MaintenanceStatus status = MaintenanceStatus.OPEN;

    @Column(name = "assigned_to", length = 200)
    private String assignedTo;

    @Column(name = "assigned_at")
    private OffsetDateTime assignedAt;

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "tenant_rating")
    private Integer tenantRating;

    @Column(name = "tenant_feedback", length = 500)
    private String tenantFeedback;

    // SLA & Escalation fields (Zolo-style)
    @Column(name = "sla_deadline_at")
    private OffsetDateTime slaDeadlineAt;

    @Column(name = "sla_breached", nullable = false)
    @Builder.Default
    private boolean slaBreached = false;

    @Column(name = "escalation_level", nullable = false)
    @Builder.Default
    private int escalationLevel = 1;

    @Column(name = "escalated_at")
    private OffsetDateTime escalatedAt;

    @Column(name = "reopened_at")
    private OffsetDateTime reopenedAt;

    @Column(name = "reopen_count", nullable = false)
    @Builder.Default
    private int reopenCount = 0;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TicketComment> comments = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
