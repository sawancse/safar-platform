package com.safar.user.entity;

import com.safar.user.entity.enums.ConnectionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "nomad_connections", schema = "users")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class NomadConnection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID requesterId;

    @Column(nullable = false)
    private UUID recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ConnectionStatus status = ConnectionStatus.PENDING;

    @CreationTimestamp
    private OffsetDateTime createdAt;
}
