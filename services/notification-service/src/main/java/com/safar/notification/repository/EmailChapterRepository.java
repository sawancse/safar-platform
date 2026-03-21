package com.safar.notification.repository;

import com.safar.notification.entity.EmailChapter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmailChapterRepository extends JpaRepository<EmailChapter, UUID> {
    List<EmailChapter> findByBookingIdOrderByChapterNumber(UUID bookingId);
    Optional<EmailChapter> findByBookingIdAndChapterNumber(UUID bookingId, int chapterNumber);
    boolean existsByBookingIdAndChapterNumber(UUID bookingId, int chapterNumber);
    int countByBookingId(UUID bookingId);
}
