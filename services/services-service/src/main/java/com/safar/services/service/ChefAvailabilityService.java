package com.safar.services.service;

import com.safar.services.entity.ChefAvailability;
import com.safar.services.entity.ChefProfile;
import com.safar.services.repository.ChefAvailabilityRepository;
import com.safar.services.repository.ChefBookingRepository;
import com.safar.services.repository.ChefProfileRepository;
import com.safar.services.repository.EventBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChefAvailabilityService {

    private final ChefAvailabilityRepository availabilityRepo;
    private final ChefProfileRepository chefProfileRepo;
    private final ChefBookingRepository bookingRepo;
    private final EventBookingRepository eventRepo;

    @Transactional
    public ChefAvailability blockDate(UUID userId, LocalDate date, String reason) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        if (availabilityRepo.existsByChefIdAndBlockedDate(chef.getId(), date)) {
            throw new IllegalArgumentException("Date already blocked");
        }

        ChefAvailability block = ChefAvailability.builder()
                .chefId(chef.getId())
                .blockedDate(date)
                .reason(reason)
                .build();
        log.info("Chef {} blocked date {}", chef.getId(), date);
        return availabilityRepo.save(block);
    }

    @Transactional
    public void unblockDate(UUID userId, LocalDate date) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));
        availabilityRepo.deleteByChefIdAndBlockedDate(chef.getId(), date);
        log.info("Chef {} unblocked date {}", chef.getId(), date);
    }

    @Transactional
    public List<ChefAvailability> bulkBlockDates(UUID userId, List<LocalDate> dates, String reason) {
        ChefProfile chef = chefProfileRepo.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Chef profile not found"));

        List<ChefAvailability> blocks = new ArrayList<>();
        for (LocalDate date : dates) {
            if (!availabilityRepo.existsByChefIdAndBlockedDate(chef.getId(), date)) {
                blocks.add(ChefAvailability.builder()
                        .chefId(chef.getId()).blockedDate(date).reason(reason).build());
            }
        }
        return availabilityRepo.saveAll(blocks);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getCalendar(UUID chefId, LocalDate from, LocalDate to) {
        List<ChefAvailability> blocked = availabilityRepo.findByChefIdAndBlockedDateBetween(chefId, from, to);
        Set<LocalDate> blockedDates = blocked.stream().map(ChefAvailability::getBlockedDate).collect(Collectors.toSet());

        // Also get booked dates from bookings
        var bookings = bookingRepo.findByChefId(chefId);
        Set<LocalDate> bookedDates = bookings.stream()
                .filter(b -> b.getServiceDate() != null && !b.getServiceDate().isBefore(from) && !b.getServiceDate().isAfter(to))
                .filter(b -> !"CANCELLED".equals(b.getStatus().name()))
                .map(b -> b.getServiceDate())
                .collect(Collectors.toSet());

        var events = eventRepo.findByChefId(chefId);
        events.stream()
                .filter(e -> e.getEventDate() != null && !e.getEventDate().isBefore(from) && !e.getEventDate().isAfter(to))
                .filter(e -> !"CANCELLED".equals(e.getStatus().name()))
                .forEach(e -> bookedDates.add(e.getEventDate()));

        Map<String, Object> calendar = new HashMap<>();
        calendar.put("blockedDates", blockedDates);
        calendar.put("bookedDates", bookedDates);
        calendar.put("from", from);
        calendar.put("to", to);
        return calendar;
    }
}
