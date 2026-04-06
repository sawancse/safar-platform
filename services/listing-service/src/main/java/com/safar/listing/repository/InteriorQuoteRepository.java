package com.safar.listing.repository;

import com.safar.listing.entity.InteriorQuote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InteriorQuoteRepository extends JpaRepository<InteriorQuote, UUID> {

    List<InteriorQuote> findByInteriorProjectIdOrderByCreatedAtDesc(UUID interiorProjectId);
}
