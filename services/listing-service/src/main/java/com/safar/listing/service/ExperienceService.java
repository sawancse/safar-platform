package com.safar.listing.service;

import com.safar.listing.dto.ExperienceBookingRequest;
import com.safar.listing.dto.ExperienceRequest;
import com.safar.listing.dto.SessionRequest;
import com.safar.listing.entity.Experience;
import com.safar.listing.entity.ExperienceBooking;
import com.safar.listing.entity.ExperienceSession;
import com.safar.listing.entity.enums.ExperienceCategory;
import com.safar.listing.entity.enums.ExperienceStatus;
import com.safar.listing.entity.enums.SessionStatus;
import com.safar.listing.repository.ExperienceBookingRepository;
import com.safar.listing.repository.ExperienceRepository;
import com.safar.listing.repository.ExperienceSessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExperienceService {

    static final int PLATFORM_COMMISSION_PCT = 20;
    private static final String ALPHANUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ExperienceRepository experienceRepository;
    private final ExperienceSessionRepository sessionRepository;
    private final ExperienceBookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public Experience createExperience(UUID hostId, ExperienceRequest req) {
        Experience experience = Experience.builder()
                .hostId(hostId)
                .title(req.title())
                .description(req.description())
                .category(req.category())
                .city(req.city())
                .locationName(req.locationName())
                .durationHours(req.durationHours())
                .maxGuests(req.maxGuests() != null ? req.maxGuests() : 10)
                .pricePaise(req.pricePaise())
                .languagesSpoken(req.languagesSpoken() != null ? req.languagesSpoken() : "en")
                .mediaUrls(req.mediaUrls() != null ? req.mediaUrls() : "")
                .status(ExperienceStatus.DRAFT)
                .reviewCount(0)
                .build();

        Experience saved = experienceRepository.save(experience);
        log.info("Experience {} created by host {}", saved.getId(), hostId);
        return saved;
    }

    @Transactional
    public ExperienceSession addSession(UUID hostId, UUID experienceId, SessionRequest req) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new NoSuchElementException("Experience not found: " + experienceId));

        if (!experience.getHostId().equals(hostId)) {
            throw new IllegalStateException("Only the host can add sessions to this experience");
        }

        ExperienceSession session = ExperienceSession.builder()
                .experienceId(experienceId)
                .sessionDate(req.sessionDate())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .availableSpots(req.availableSpots())
                .bookedSpots(0)
                .status(SessionStatus.OPEN)
                .build();

        ExperienceSession saved = sessionRepository.save(session);
        log.info("Session {} added to experience {} by host {}", saved.getId(), experienceId, hostId);
        return saved;
    }

    @Transactional
    public ExperienceBooking bookExperience(UUID guestId, ExperienceBookingRequest req) {
        ExperienceSession session = sessionRepository.findById(req.sessionId())
                .orElseThrow(() -> new NoSuchElementException("Session not found: " + req.sessionId()));

        if (session.getStatus() == SessionStatus.FULL) {
            throw new IllegalStateException("Session is full");
        }
        if (session.getStatus() == SessionStatus.CANCELLED) {
            throw new IllegalStateException("Session is cancelled");
        }

        int spotsAfter = session.getBookedSpots() + req.numGuests();
        if (spotsAfter > session.getAvailableSpots()) {
            throw new IllegalStateException(
                    String.format("Not enough spots: %d available, %d requested",
                            session.getAvailableSpots() - session.getBookedSpots(), req.numGuests()));
        }

        Experience experience = experienceRepository.findById(session.getExperienceId())
                .orElseThrow(() -> new NoSuchElementException(
                        "Experience not found: " + session.getExperienceId()));

        long totalPaise = experience.getPricePaise() * req.numGuests();
        long platformFee = totalPaise * PLATFORM_COMMISSION_PCT / 100;
        long hostPayout = totalPaise - platformFee;

        String ref = "SAF-EXP-" + generateAlphaNum(6);

        ExperienceBooking booking = ExperienceBooking.builder()
                .experienceId(experience.getId())
                .sessionId(session.getId())
                .guestId(guestId)
                .propertyBookingId(req.propertyBookingId())
                .numGuests(req.numGuests())
                .totalPaise(totalPaise)
                .platformFeePaise(platformFee)
                .hostPayoutPaise(hostPayout)
                .status("CONFIRMED")
                .ref(ref)
                .build();

        ExperienceBooking saved = bookingRepository.save(booking);

        session.setBookedSpots(spotsAfter);
        if (spotsAfter >= session.getAvailableSpots()) {
            session.setStatus(SessionStatus.FULL);
        }
        sessionRepository.save(session);

        String payload = String.format(
                "{\"bookingId\":\"%s\",\"experienceId\":\"%s\",\"guestId\":\"%s\",\"ref\":\"%s\",\"totalPaise\":%d}",
                saved.getId(), experience.getId(), guestId, ref, totalPaise);
        kafkaTemplate.send("experience.booked", saved.getId().toString(), payload);
        log.info("Experience booking {} created: ref={}, totalPaise={}", saved.getId(), ref, totalPaise);

        return saved;
    }

    public Page<Experience> searchExperiences(String city, String category, Pageable pageable) {
        if (city != null && category != null) {
            ExperienceCategory cat = ExperienceCategory.valueOf(category.toUpperCase());
            return experienceRepository.findByCityAndCategoryAndStatus(
                    city, cat, ExperienceStatus.ACTIVE, pageable);
        } else if (city != null) {
            return experienceRepository.findByCityAndStatus(city, ExperienceStatus.ACTIVE, pageable);
        } else {
            return experienceRepository.findByStatus(ExperienceStatus.ACTIVE, pageable);
        }
    }

    public Experience getExperience(UUID experienceId) {
        return experienceRepository.findById(experienceId)
                .orElseThrow(() -> new NoSuchElementException("Experience not found: " + experienceId));
    }

    public List<ExperienceBooking> getMyBookings(UUID guestId) {
        return bookingRepository.findByGuestId(guestId);
    }

    private String generateAlphaNum(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
