package com.safar.payment.repository;

import com.safar.payment.entity.DonationStats;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DonationStatsRepository extends JpaRepository<DonationStats, UUID> {
}
