package com.safar.payment.repository;

import com.safar.payment.entity.GstInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GstInvoiceRepository extends JpaRepository<GstInvoice, UUID> {
    List<GstInvoice> findByHostId(UUID hostId);
    List<GstInvoice> findByHostIdAndInvoiceDateBetween(UUID hostId, java.time.LocalDate start, java.time.LocalDate end);
    boolean existsByBookingId(UUID bookingId);
    Optional<GstInvoice> findFirstByBookingId(UUID bookingId);
}
