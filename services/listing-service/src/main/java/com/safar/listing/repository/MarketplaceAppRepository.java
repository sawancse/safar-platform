package com.safar.listing.repository;

import com.safar.listing.entity.MarketplaceApp;
import com.safar.listing.entity.enums.AppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MarketplaceAppRepository extends JpaRepository<MarketplaceApp, UUID> {

    List<MarketplaceApp> findByDeveloperId(UUID developerId);

    Page<MarketplaceApp> findByStatus(AppStatus status, Pageable pageable);
}
