package com.safar.payment.repository;

import com.safar.payment.entity.RefundRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RefundRecordRepository extends JpaRepository<RefundRecord, UUID> {
    List<RefundRecord> findByPaymentId(UUID paymentId);
    List<RefundRecord> findByBookingId(UUID bookingId);
}
