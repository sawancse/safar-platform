package com.safar.booking.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "booking_room_selections", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingRoomSelection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "room_type_id", nullable = false)
    private UUID roomTypeId;

    @Column(name = "room_type_name", nullable = false)
    private String roomTypeName;

    @Builder.Default
    private Integer count = 1;

    @Column(name = "price_per_unit_paise", nullable = false)
    private Long pricePerUnitPaise;

    @Column(name = "total_paise", nullable = false)
    private Long totalPaise;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
