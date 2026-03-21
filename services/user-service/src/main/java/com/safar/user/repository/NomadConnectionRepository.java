package com.safar.user.repository;

import com.safar.user.entity.NomadConnection;
import com.safar.user.entity.enums.ConnectionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NomadConnectionRepository extends JpaRepository<NomadConnection, UUID> {
    boolean existsByRequesterIdAndRecipientId(UUID requesterId, UUID recipientId);
    Optional<NomadConnection> findByIdAndRecipientId(UUID id, UUID recipientId);
    List<NomadConnection> findByRequesterIdAndStatusOrRecipientIdAndStatus(
            UUID requesterId, ConnectionStatus status1,
            UUID recipientId, ConnectionStatus status2);
}
