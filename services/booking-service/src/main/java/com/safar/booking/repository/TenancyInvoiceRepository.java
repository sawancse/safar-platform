package com.safar.booking.repository;

import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TenancyInvoiceRepository extends JpaRepository<TenancyInvoice, UUID> {

    Page<TenancyInvoice> findByTenancyIdOrderByBillingYearDescBillingMonthDesc(UUID tenancyId, Pageable pageable);

    List<TenancyInvoice> findByStatus(InvoiceStatus status);

    boolean existsByTenancyIdAndBillingMonthAndBillingYear(UUID tenancyId, int month, int year);
}
