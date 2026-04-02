package com.safar.listing.service;

import com.safar.listing.client.UserNameClient;
import com.safar.listing.dto.ExperienceBookingRequest;
import com.safar.listing.dto.ExperienceRequest;
import com.safar.listing.dto.ExperienceResponse;
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

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;

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
    private final UserNameClient userNameClient;

    @Transactional
    public ExperienceResponse createExperience(UUID hostId, ExperienceRequest req) {
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
        return toResponse(saved, userNameClient.getUserName(hostId));
    }

    @Transactional
    public ExperienceResponse updateStatus(UUID hostId, UUID experienceId, ExperienceStatus newStatus) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new NoSuchElementException("Experience not found: " + experienceId));

        if (!experience.getHostId().equals(hostId)) {
            throw new IllegalStateException("Only the host can update this experience");
        }

        ExperienceStatus current = experience.getStatus();
        boolean valid = switch (newStatus) {
            case ACTIVE -> current == ExperienceStatus.DRAFT || current == ExperienceStatus.PAUSED;
            case PAUSED -> current == ExperienceStatus.ACTIVE;
            case DRAFT -> current == ExperienceStatus.PAUSED;
            default -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s", current, newStatus));
        }

        experience.setStatus(newStatus);
        Experience saved = experienceRepository.save(experience);
        log.info("Experience {} status changed {} → {} by host {}", experienceId, current, newStatus, hostId);
        return toResponse(saved, userNameClient.getUserName(hostId));
    }

    @Transactional
    public ExperienceResponse updateExperience(UUID hostId, UUID experienceId, ExperienceRequest req) {
        Experience experience = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new NoSuchElementException("Experience not found: " + experienceId));

        if (!experience.getHostId().equals(hostId)) {
            throw new IllegalStateException("Only the host can update this experience");
        }

        experience.setTitle(req.title());
        experience.setDescription(req.description());
        experience.setCategory(req.category());
        experience.setCity(req.city());
        experience.setLocationName(req.locationName());
        experience.setDurationHours(req.durationHours());
        if (req.maxGuests() != null) experience.setMaxGuests(req.maxGuests());
        experience.setPricePaise(req.pricePaise());
        if (req.languagesSpoken() != null) experience.setLanguagesSpoken(req.languagesSpoken());
        if (req.mediaUrls() != null) experience.setMediaUrls(req.mediaUrls());

        Experience saved = experienceRepository.save(experience);
        log.info("Experience {} updated by host {}", experienceId, hostId);
        return toResponse(saved, userNameClient.getUserName(hostId));
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

    public Page<ExperienceResponse> searchExperiences(String city, String category, Pageable pageable) {
        Page<Experience> page;
        if (city != null && category != null) {
            ExperienceCategory cat = ExperienceCategory.valueOf(category.toUpperCase());
            page = experienceRepository.findByCityAndCategoryAndStatus(
                    city, cat, ExperienceStatus.ACTIVE, pageable);
        } else if (city != null) {
            page = experienceRepository.findByCityAndStatus(city, ExperienceStatus.ACTIVE, pageable);
        } else if (category != null) {
            ExperienceCategory cat = ExperienceCategory.valueOf(category.toUpperCase());
            page = experienceRepository.findByCategoryAndStatus(cat, ExperienceStatus.ACTIVE, pageable);
        } else {
            page = experienceRepository.findByStatus(ExperienceStatus.ACTIVE, pageable);
        }
        Map<UUID, String> hostNames = resolveHostNames(page.getContent());
        return page.map(e -> toResponse(e, hostNames.getOrDefault(e.getHostId(), "Host")));
    }

    public ExperienceResponse getExperience(UUID experienceId) {
        Experience e = experienceRepository.findById(experienceId)
                .orElseThrow(() -> new NoSuchElementException("Experience not found: " + experienceId));
        return toResponse(e, userNameClient.getUserName(e.getHostId()));
    }

    public List<ExperienceResponse> getHostExperiences(UUID hostId) {
        List<Experience> list = experienceRepository.findByHostId(hostId);
        String hostName = userNameClient.getUserName(hostId);
        return list.stream().map(e -> toResponse(e, hostName)).toList();
    }

    public List<ExperienceBooking> getMyBookings(UUID guestId) {
        return bookingRepository.findByGuestId(guestId);
    }

    private ExperienceResponse toResponse(Experience e, String hostName) {
        int durationMinutes = e.getDurationHours() != null
                ? e.getDurationHours().multiply(BigDecimal.valueOf(60)).intValue()
                : 0;

        return new ExperienceResponse(
                e.getId(),
                e.getHostId(),
                e.getTitle(),
                e.getDescription(),
                e.getCategory(),
                e.getCity(),
                e.getLocationName(),
                durationMinutes,
                e.getMaxGuests(),
                e.getPricePaise(),
                e.getLanguagesSpoken(),
                e.getMediaUrls(),
                e.getStatus(),
                e.getRating(),
                e.getReviewCount() != null ? e.getReviewCount() : 0,
                hostName,
                e.getCreatedAt()
        );
    }

    private Map<UUID, String> resolveHostNames(List<Experience> experiences) {
        Set<UUID> hostIds = experiences.stream()
                .map(Experience::getHostId)
                .collect(Collectors.toSet());
        Map<UUID, String> names = new HashMap<>();
        for (UUID hostId : hostIds) {
            names.put(hostId, userNameClient.getUserName(hostId));
        }
        return names;
    }

    private String generateAlphaNum(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUM.charAt(RANDOM.nextInt(ALPHANUM.length())));
        }
        return sb.toString();
    }
}
