package com.safar.booking.entity;

import com.safar.booking.entity.enums.RecurringFrequency;
import com.safar.booking.entity.enums.RecurringStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "recurring_bookings", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurringBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID guestId;

    @Column(nullable = false)
    private UUID listingId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RecurringFrequency frequency;

    @Column(nullable = false)
    private LocalTime checkInTime;

    @Column(nullable = false)
    private LocalTime checkOutTime;

    private Integer dayOfWeek;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private RecurringStatus status = RecurringStatus.ACTIVE;

    @Builder.Default
    @Column(nullable = false)
    private Integer noticeDays = 7;

    @Builder.Default
    @Column(nullable = false)
    private Long totalPaidPaise = 0L;

    private LocalDate nextBookingDate;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
