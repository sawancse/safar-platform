package com.safar.listing.repository;

import com.safar.listing.entity.PartnerBank;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PartnerBankRepository extends JpaRepository<PartnerBank, UUID> {

    List<PartnerBank> findByActiveTrue();

    List<PartnerBank> findByActiveTrueOrderByInterestRateMinAsc();
}
