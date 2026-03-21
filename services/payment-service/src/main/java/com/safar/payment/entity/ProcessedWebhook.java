package com.safar.payment.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "processed_webhooks", schema = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedWebhook {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 200)
    private String gatewayEventId;

    @Column(nullable = false, length = 20)
    private String gateway;

    @Column(length = 100)
    private String eventType;

    @Builder.Default
    @Column(nullable = false, length = 20)
    private String status = "PROCESSING";

    @Builder.Default
    private OffsetDateTime processedAt = OffsetDateTime.now();
}
