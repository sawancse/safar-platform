package com.safar.payment.repository;

import com.safar.payment.entity.HostInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface HostInvoiceRepository extends JpaRepository<HostInvoice, UUID> {
    List<HostInvoice> findByHostId(UUID hostId);
    Optional<HostInvoice> findByRazorpaySubId(String razorpaySubId);
}
