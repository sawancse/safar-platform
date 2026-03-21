package com.safar.messaging.repository;

import com.safar.messaging.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    List<Conversation> findByParticipant1IdOrParticipant2IdOrderByLastMessageAtDesc(UUID participant1Id, UUID participant2Id);

    Optional<Conversation> findByParticipant1IdAndParticipant2IdAndListingId(UUID participant1Id, UUID participant2Id, UUID listingId);
}
