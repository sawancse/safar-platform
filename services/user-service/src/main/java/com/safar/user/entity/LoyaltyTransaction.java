package com.safar.user.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "loyalty_transactions", schema = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Long points;

    @Column(nullable = false, length = 20)
    private String type; // EARN, REDEEM, BONUS, EXPIRE

    @Column(length = 200)
    private String description;

    @Column(name = "booking_id")
    private UUID bookingId;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
