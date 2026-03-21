package com.safar.notification.repository;

import com.safar.notification.entity.ScheduledEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ScheduledEmailRepository extends JpaRepository<ScheduledEmail, UUID> {
    @Query("SELECT s FROM ScheduledEmail s WHERE s.sent = false AND s.cancelled = false AND s.scheduledFor <= :now")
    List<ScheduledEmail> findPendingEmails(OffsetDateTime now);

    @Modifying
    @Query("UPDATE ScheduledEmail s SET s.cancelled = true WHERE s.bookingId = :bookingId AND s.sent = false")
    int cancelByBookingId(UUID bookingId);

    List<ScheduledEmail> findByBookingIdAndSentFalseAndCancelledFalse(UUID bookingId);
}
