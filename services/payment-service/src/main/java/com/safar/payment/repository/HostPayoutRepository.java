package com.safar.payment.repository;

import com.safar.payment.entity.HostPayout;
import com.safar.payment.entity.enums.PayoutStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HostPayoutRepository extends JpaRepository<HostPayout, UUID> {

    Page<HostPayout> findByHostIdOrderByCreatedAtDesc(UUID hostId, Pageable pageable);

    List<HostPayout> findByPayoutStatus(PayoutStatus status);

    @Query("SELECT h FROM HostPayout h WHERE h.hostId = :hostId " +
           "AND MONTH(h.createdAt) = :month AND YEAR(h.createdAt) = :year")
    List<HostPayout> findByHostIdAndMonth(UUID hostId, int month, int year);

    boolean existsByInvoiceId(UUID invoiceId);
}
