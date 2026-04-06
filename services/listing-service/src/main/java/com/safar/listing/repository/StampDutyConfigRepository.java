package com.safar.listing.repository;

import com.safar.listing.entity.StampDutyConfig;
import com.safar.listing.entity.enums.AgreementType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StampDutyConfigRepository extends JpaRepository<StampDutyConfig, UUID> {

    Optional<StampDutyConfig> findByStateAndAgreementTypeAndActiveTrue(String state, AgreementType type);
}
