package com.safar.booking.entity;

import com.safar.booking.entity.enums.MilesTier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "miles_balance", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MilesBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID userId;

    @Builder.Default
    @Column(nullable = false)
    private Long balance = 0L;

    @Builder.Default
    @Column(nullable = false)
    private Long lifetime = 0L;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false, length = 20)
    private MilesTier tier = MilesTier.BRONZE;

    @UpdateTimestamp
    private OffsetDateTime updatedAt;
}
