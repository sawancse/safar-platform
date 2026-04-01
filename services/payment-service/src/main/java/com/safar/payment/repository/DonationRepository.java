package com.safar.payment.repository;

import com.safar.payment.entity.Donation;
import com.safar.payment.entity.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DonationRepository extends JpaRepository<Donation, UUID> {

    Optional<Donation> findByRazorpayOrderId(String razorpayOrderId);

    Optional<Donation> findByDonationRef(String donationRef);

    List<Donation> findByDonorIdAndStatusOrderByCreatedAtDesc(UUID donorId, DonationStatus status);

    Page<Donation> findByDonorIdOrderByCreatedAtDesc(UUID donorId, Pageable pageable);

    Page<Donation> findByStatusOrderByCreatedAtDesc(DonationStatus status, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT d.donorEmail) FROM Donation d WHERE d.status = 'CAPTURED'")
    int countUniqueDonors();

    @Query("SELECT COALESCE(SUM(d.amountPaise), 0) FROM Donation d WHERE d.status = 'CAPTURED'")
    long sumCapturedAmount();

    @Query("SELECT d FROM Donation d WHERE d.status = 'CAPTURED' ORDER BY d.capturedAt DESC")
    List<Donation> findRecentDonations(Pageable pageable);

    long countByStatus(DonationStatus status);
}
