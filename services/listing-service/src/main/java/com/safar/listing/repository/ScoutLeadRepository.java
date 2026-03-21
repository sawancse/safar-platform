package com.safar.listing.repository;

import com.safar.listing.entity.ScoutLead;
import com.safar.listing.entity.enums.ScoutLeadStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ScoutLeadRepository extends JpaRepository<ScoutLead, UUID> {
    List<ScoutLead> findByCity(String city);
    List<ScoutLead> findByStatus(ScoutLeadStatus status);
    Page<ScoutLead> findAll(Pageable pageable);
}
