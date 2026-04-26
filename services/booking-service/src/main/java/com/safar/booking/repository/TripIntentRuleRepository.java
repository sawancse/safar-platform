package com.safar.booking.repository;

import com.safar.booking.entity.TripIntentRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TripIntentRuleRepository extends JpaRepository<TripIntentRule, UUID> {

    /**
     * Fetch all enabled rules that apply to the given country, ordered by
     * priority (lowest = highest priority first). The evaluator iterates
     * this list and applies the matching logic.
     *
     * Country filter uses PostgreSQL array containment — a rule with
     * {@code applies_to_country = ARRAY['IN','AE']} matches when called
     * with country='IN' OR country='AE'.
     */
    @Query(value = """
            SELECT * FROM bookings.trip_intent_rules
             WHERE enabled = true
               AND :country = ANY(applies_to_country)
             ORDER BY priority ASC
            """, nativeQuery = true)
    List<TripIntentRule> findEnabledForCountryByPriority(@Param("country") String country);
}
