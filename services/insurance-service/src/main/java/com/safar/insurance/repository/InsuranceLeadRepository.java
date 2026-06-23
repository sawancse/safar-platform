package com.safar.insurance.repository;

import com.safar.insurance.entity.InsuranceLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface InsuranceLeadRepository extends JpaRepository<InsuranceLead, UUID> {
    Page<InsuranceLead> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<InsuranceLead> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);
}
