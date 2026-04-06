package com.safar.booking.repository;

import com.safar.booking.entity.TicketComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketComment, UUID> {

    List<TicketComment> findByRequestIdOrderByCreatedAtAsc(UUID requestId);

    long countByRequestId(UUID requestId);
}
