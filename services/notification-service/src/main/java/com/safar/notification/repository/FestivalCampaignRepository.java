package com.safar.notification.repository;

import com.safar.notification.entity.FestivalCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FestivalCampaignRepository extends JpaRepository<FestivalCampaign, UUID> {
    List<FestivalCampaign> findByFestivalDateBetweenAndIsActiveTrue(LocalDate from, LocalDate to);

    @Query("SELECT f FROM FestivalCampaign f WHERE f.festivalDate = :date AND f.isActive = true AND (f.region IS NULL OR f.region = :region)")
    List<FestivalCampaign> findActiveByDateAndRegion(LocalDate date, String region);

    List<FestivalCampaign> findByFestivalDateAndIsActiveTrue(LocalDate date);
}
