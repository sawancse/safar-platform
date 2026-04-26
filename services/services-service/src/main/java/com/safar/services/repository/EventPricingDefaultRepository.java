package com.safar.services.repository;

import com.safar.services.entity.EventPricingDefault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventPricingDefaultRepository extends JpaRepository<EventPricingDefault, UUID> {

    List<EventPricingDefault> findByActiveTrueOrderByCategoryAscSortOrderAsc();

    List<EventPricingDefault> findByCategoryAndActiveTrueOrderBySortOrderAsc(String category);

    Optional<EventPricingDefault> findByItemKey(String itemKey);
}
