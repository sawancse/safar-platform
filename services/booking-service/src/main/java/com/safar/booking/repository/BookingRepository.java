package com.safar.booking.repository;

import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID>, JpaSpecificationExecutor<Booking> {
    List<Booking> findByGuestId(UUID guestId);
    List<Booking> findByHostId(UUID hostId);
    List<Booking> findByListingIdAndStatusIn(UUID listingId, List<BookingStatus> statuses);
    List<Booking> findByGroupBookingId(UUID groupBookingId);
    List<Booking> findByHostIdAndCheckInBetween(UUID hostId, LocalDateTime from, LocalDateTime to);
    List<Booking> findByStatus(BookingStatus status);
    List<Booking> findByStatusInAndCheckOutBefore(List<BookingStatus> statuses, LocalDateTime checkOut);
    Page<Booking> findBySecurityDepositStatusIn(List<String> statuses, Pageable pageable);

    // PARTIAL_PREPAID bookings with a remaining balance whose check-in falls in [from, to).
    // tier = "T7" or "T1" picks which dedup timestamp column to require to be NULL.
    @Query("""
            SELECT b FROM Booking b
            WHERE b.paymentMode = 'PARTIAL_PREPAID'
              AND b.status = com.safar.booking.entity.enums.BookingStatus.CONFIRMED
              AND b.dueAtPropertyPaise IS NOT NULL
              AND b.dueAtPropertyPaise > 0
              AND b.checkIn >= :from
              AND b.checkIn < :to
              AND ((:tier = 'T7' AND b.balanceReminderT7SentAt IS NULL)
                OR (:tier = 'T1' AND b.balanceReminderT1SentAt IS NULL))
            """)
    List<Booking> findBalanceReminderCandidates(@Param("from") LocalDateTime from,
                                                @Param("to") LocalDateTime to,
                                                @Param("tier") String tier);

    // Realized gross revenue: total transacted value of bookings that reached a paid
    // state (CONFIRMED/CHECKED_IN/COMPLETED). Excludes DRAFT/PENDING_PAYMENT/CANCELLED/NO_SHOW.
    @Query("""
            SELECT COALESCE(SUM(b.totalAmountPaise), 0) FROM Booking b
            WHERE b.status IN (
                com.safar.booking.entity.enums.BookingStatus.CONFIRMED,
                com.safar.booking.entity.enums.BookingStatus.CHECKED_IN,
                com.safar.booking.entity.enums.BookingStatus.COMPLETED)
            """)
    long sumRealizedRevenuePaise();
}
