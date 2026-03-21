package com.safar.listing.repository;

import com.safar.listing.entity.LocationSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LocationSuggestionRepository extends JpaRepository<LocationSuggestion, UUID> {

    @Query("SELECT ls FROM LocationSuggestion ls WHERE LOWER(ls.name) LIKE LOWER(CONCAT(:query, '%')) " +
           "OR LOWER(ls.displayName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "ORDER BY ls.popularityScore DESC")
    List<LocationSuggestion> findByNamePrefix(String query);

    List<LocationSuggestion> findByCityIgnoreCaseOrderByPopularityScoreDesc(String city);

    List<LocationSuggestion> findByTypeOrderByPopularityScoreDesc(String type);
}
