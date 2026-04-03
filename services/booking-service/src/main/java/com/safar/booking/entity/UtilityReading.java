package com.safar.booking.entity;

import com.safar.booking.entity.enums.UtilityType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "utility_readings", schema = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UtilityReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenancy_id", nullable = false)
    private UUID tenancyId;

    @Enumerated(EnumType.STRING)
    @Column(name = "utility_type", nullable = false, length = 20)
    private UtilityType utilityType;

    @Column(name = "reading_date", nullable = false)
    private LocalDate readingDate;

    @Column(name = "meter_number", length = 50)
    private String meterNumber;

    @Column(name = "previous_reading", nullable = false, precision = 10, scale = 2)
    private BigDecimal previousReading;

    @Column(name = "current_reading", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentReading;

    @Column(name = "units_consumed", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitsConsumed;

    @Column(name = "rate_per_unit_paise", nullable = false)
    private long ratePerUnitPaise;

    @Column(name = "total_charge_paise", nullable = false)
    private long totalChargePaise;

    @Column(name = "billing_month")
    private Integer billingMonth;

    @Column(name = "billing_year")
    private Integer billingYear;

    @Column(name = "invoice_id")
    private UUID invoiceId;

    @Column(name = "photo_url", length = 500)
    private String photoUrl;

    @Column(name = "recorded_by", nullable = false, length = 20)
    @Builder.Default
    private String recordedBy = "HOST";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
