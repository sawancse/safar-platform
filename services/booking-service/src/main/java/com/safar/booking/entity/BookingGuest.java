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
@Table(name = "booking_guests", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingGuest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "booking_id", nullable = false)
    private UUID bookingId;

    @Column(name = "full_name", nullable = false, length = 200)
    private String fullName;

    @Column(length = 200)
    private String email;

    @Column(length = 20)
    private String phone;

    private Integer age;

    @Column(name = "id_type", length = 30)
    private String idType; // AADHAAR, PASSPORT, DRIVING_LICENSE, VOTER_ID

    @Column(name = "id_number", length = 50)
    private String idNumber;

    @Column(name = "room_assignment", length = 100)
    private String roomAssignment;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
