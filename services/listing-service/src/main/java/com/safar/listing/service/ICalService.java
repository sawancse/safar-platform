package com.safar.listing.service;

import com.safar.listing.entity.Availability;
import com.safar.listing.entity.ICalFeed;
import com.safar.listing.repository.AvailabilityRepository;
import com.safar.listing.repository.ICalFeedRepository;
import com.safar.listing.repository.ListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ICalService {

    private final ICalFeedRepository icalFeedRepository;
    private final AvailabilityRepository availabilityRepository;
    private final ListingRepository listingRepository;

    private static final DateTimeFormatter ICAL_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /**
     * Export listing availability as iCal VCALENDAR string.
     */
    public String exportCalendar(UUID listingId) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }

        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(365);
        List<Availability> blocked = availabilityRepository.findByListingIdAndDateBetween(listingId, from, to)
                .stream()
                .filter(a -> Boolean.FALSE.equals(a.getIsAvailable()))
                .toList();

        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n");
        sb.append("VERSION:2.0\r\n");
        sb.append("PRODID:-//Safar//Availability//EN\r\n");
        sb.append("CALSCALE:GREGORIAN\r\n");
        sb.append("METHOD:PUBLISH\r\n");
        sb.append("X-WR-CALNAME:Safar Calendar\r\n");

        for (Availability av : blocked) {
            sb.append("BEGIN:VEVENT\r\n");
            sb.append("DTSTART;VALUE=DATE:").append(av.getDate().format(ICAL_DATE_FMT)).append("\r\n");
            sb.append("DTEND;VALUE=DATE:").append(av.getDate().plusDays(1).format(ICAL_DATE_FMT)).append("\r\n");
            sb.append("SUMMARY:Blocked - Safar\r\n");
            sb.append("UID:").append(av.getId()).append("@safar.com\r\n");
            sb.append("DTSTAMP:").append(OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'"))).append("\r\n");
            sb.append("END:VEVENT\r\n");
        }

        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    /**
     * Import (save) an iCal feed URL for a listing.
     * Auto-detects source platform from URL.
     */
    @Transactional
    public ICalFeed importFeed(UUID listingId, UUID hostId, String feedUrl, String feedName) {
        if (!listingRepository.existsById(listingId)) {
            throw new NoSuchElementException("Listing not found: " + listingId);
        }

        String platform = detectPlatform(feedUrl);

        ICalFeed feed = ICalFeed.builder()
                .listingId(listingId)
                .feedUrl(feedUrl)
                .feedName(feedName != null ? feedName : platform + " Calendar")
                .sourcePlatform(platform)
                .build();

        return icalFeedRepository.save(feed);
    }

    /**
     * Sync a specific feed with source-aware conflict resolution.
     * - Blocks dates found in the feed with source = "ICAL:<feedId>"
     * - Unblocks dates previously blocked by THIS feed but no longer in the feed
     * - Does NOT touch dates blocked by other sources (MANUAL, BOOKING, other feeds)
     */
    @Transactional
    public void syncFeed(UUID feedId) {
        ICalFeed feed = icalFeedRepository.findById(feedId)
                .orElseThrow(() -> new NoSuchElementException("Feed not found: " + feedId));

        String sourceTag = "ICAL:" + feedId;

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15))
                    .build();

            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(feed.getFeedUrl()))
                    .timeout(Duration.ofSeconds(30))
                    .GET();

            // Conditional fetch with ETag/Last-Modified
            if (feed.getEtag() != null) {
                reqBuilder.header("If-None-Match", feed.getEtag());
            }
            if (feed.getLastModifiedHeader() != null) {
                reqBuilder.header("If-Modified-Since", feed.getLastModifiedHeader());
            }

            HttpResponse<String> response = client.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());

            // 304 Not Modified — nothing changed
            if (response.statusCode() == 304) {
                feed.setLastSyncedAt(OffsetDateTime.now());
                feed.setLastSyncStatus("SUCCESS");
                feed.setSyncFailureCount(0);
                icalFeedRepository.save(feed);
                log.debug("Feed {} not modified (304)", feedId);
                return;
            }

            if (response.statusCode() != 200) {
                handleSyncFailure(feed, "HTTP " + response.statusCode());
                return;
            }

            // Store ETag/Last-Modified for next conditional fetch
            response.headers().firstValue("ETag").ifPresent(feed::setEtag);
            response.headers().firstValue("Last-Modified").ifPresent(feed::setLastModifiedHeader);

            // Parse blocked dates from feed
            Set<LocalDate> feedBlockedDates = new HashSet<>(parseVEvents(response.body()));
            log.info("Parsed {} blocked dates from feed {} ({})", feedBlockedDates.size(), feedId, feed.getFeedName());

            // Get all dates currently blocked by THIS feed
            List<Availability> currentlyBlockedByFeed = availabilityRepository
                    .findByListingIdAndSourceStartingWith(feed.getListingId(), sourceTag);

            Set<LocalDate> currentlyBlockedDates = currentlyBlockedByFeed.stream()
                    .map(Availability::getDate)
                    .collect(Collectors.toSet());

            // Block new dates from feed
            Set<LocalDate> datesToBlock = new HashSet<>(feedBlockedDates);
            datesToBlock.removeAll(currentlyBlockedDates); // only new dates

            for (LocalDate date : datesToBlock) {
                Availability av = availabilityRepository
                        .findByListingIdAndDate(feed.getListingId(), date)
                        .orElse(Availability.builder()
                                .listingId(feed.getListingId())
                                .date(date)
                                .build());
                av.setIsAvailable(false);
                av.setSource(sourceTag);
                availabilityRepository.save(av);
            }

            // Unblock dates no longer in feed (source-aware: only unblock OUR dates)
            Set<LocalDate> datesToUnblock = new HashSet<>(currentlyBlockedDates);
            datesToUnblock.removeAll(feedBlockedDates); // dates we blocked but feed no longer has

            for (LocalDate date : datesToUnblock) {
                availabilityRepository.findByListingIdAndDate(feed.getListingId(), date)
                        .ifPresent(av -> {
                            // Only unblock if we (this feed) were the blocker
                            if (sourceTag.equals(av.getSource())) {
                                av.setIsAvailable(true);
                                av.setSource(null);
                                availabilityRepository.save(av);
                            }
                        });
            }

            // Update feed metadata
            feed.setLastSyncedAt(OffsetDateTime.now());
            feed.setLastSyncStatus("SUCCESS");
            feed.setLastErrorMessage(null);
            feed.setSyncFailureCount(0);
            icalFeedRepository.save(feed);

            log.info("Feed {} synced: {} new blocks, {} unblocks", feedId, datesToBlock.size(), datesToUnblock.size());

        } catch (Exception e) {
            handleSyncFailure(feed, e.getMessage());
            log.error("Error syncing iCal feed {}: {}", feedId, e.getMessage(), e);
        }
    }

    /**
     * Sync all feeds that are due (based on their sync interval).
     */
    public void syncDueFeeds() {
        List<ICalFeed> activeFeeds = icalFeedRepository.findByIsActiveTrue();
        log.info("Checking {} active iCal feeds for sync", activeFeeds.size());

        int synced = 0;
        for (ICalFeed feed : activeFeeds) {
            if (isDue(feed)) {
                // Exponential backoff for failing feeds
                if (feed.getSyncFailureCount() != null && feed.getSyncFailureCount() > 0) {
                    int backoffMinutes = (int) Math.min(
                            Math.pow(2, feed.getSyncFailureCount()) * 15, // 15, 30, 60, 120, 240...
                            1440 // cap at 24 hours
                    );
                    if (feed.getLastSyncedAt() != null &&
                            feed.getLastSyncedAt().plusMinutes(backoffMinutes).isAfter(OffsetDateTime.now())) {
                        continue; // skip — still in backoff window
                    }
                }
                syncFeed(feed.getId());
                synced++;
            }
        }
        log.info("Synced {}/{} due feeds", synced, activeFeeds.size());
    }

    /**
     * Sync all active feeds (force sync, ignoring schedule).
     */
    public void syncAllFeeds() {
        List<ICalFeed> activeFeeds = icalFeedRepository.findByIsActiveTrue();
        log.info("Force syncing {} active iCal feeds", activeFeeds.size());
        for (ICalFeed feed : activeFeeds) {
            syncFeed(feed.getId());
        }
    }

    public List<ICalFeed> getFeeds(UUID listingId) {
        return icalFeedRepository.findByListingId(listingId);
    }

    @Transactional
    public void deleteFeed(UUID feedId, UUID hostId) {
        ICalFeed feed = icalFeedRepository.findById(feedId)
                .orElseThrow(() -> new NoSuchElementException("Feed not found: " + feedId));

        // Unblock all dates blocked by this feed
        String sourceTag = "ICAL:" + feedId;
        List<Availability> blockedByFeed = availabilityRepository
                .findByListingIdAndSourceStartingWith(feed.getListingId(), sourceTag);
        for (Availability av : blockedByFeed) {
            av.setIsAvailable(true);
            av.setSource(null);
            availabilityRepository.save(av);
        }
        log.info("Unblocked {} dates from deleted feed {}", blockedByFeed.size(), feedId);

        icalFeedRepository.delete(feed);
    }

    // ── Private helpers ─────────────────────────────────────────

    private boolean isDue(ICalFeed feed) {
        if (feed.getLastSyncedAt() == null) return true;
        int intervalHours = feed.getSyncIntervalHours() != null ? feed.getSyncIntervalHours() : 6;
        return feed.getLastSyncedAt().plusHours(intervalHours).isBefore(OffsetDateTime.now());
    }

    private void handleSyncFailure(ICalFeed feed, String errorMessage) {
        feed.setLastSyncedAt(OffsetDateTime.now());
        feed.setLastSyncStatus("FAILED");
        feed.setLastErrorMessage(errorMessage);
        feed.setSyncFailureCount((feed.getSyncFailureCount() != null ? feed.getSyncFailureCount() : 0) + 1);

        // Deactivate after 10 consecutive failures
        if (feed.getSyncFailureCount() >= 10) {
            feed.setIsActive(false);
            log.warn("Feed {} deactivated after {} consecutive failures", feed.getId(), feed.getSyncFailureCount());
        }

        icalFeedRepository.save(feed);
    }

    private String detectPlatform(String feedUrl) {
        if (feedUrl == null) return "CUSTOM";
        String lower = feedUrl.toLowerCase();
        if (lower.contains("airbnb.com")) return "AIRBNB";
        if (lower.contains("booking.com")) return "BOOKING";
        if (lower.contains("google.com") || lower.contains("googleapis.com")) return "GOOGLE";
        if (lower.contains("makemytrip.com")) return "MMT";
        if (lower.contains("oyorooms.com")) return "OYO";
        return "CUSTOM";
    }

    /**
     * Parse VEVENT entries from an iCal string to extract blocked dates.
     * Handles timezone conversion (UTC -> IST) for datetime-format entries.
     */
    private List<LocalDate> parseVEvents(String icalContent) {
        List<LocalDate> dates = new ArrayList<>();
        String[] lines = icalContent.split("\\r?\\n");
        LocalDate dtStart = null;
        LocalDate dtEnd = null;
        boolean inEvent = false;

        for (String line : lines) {
            String trimmed = line.trim();
            if ("BEGIN:VEVENT".equals(trimmed)) {
                inEvent = true;
                dtStart = null;
                dtEnd = null;
            } else if ("END:VEVENT".equals(trimmed)) {
                if (inEvent && dtStart != null) {
                    LocalDate end = dtEnd != null ? dtEnd : dtStart.plusDays(1);
                    LocalDate current = dtStart;
                    while (current.isBefore(end)) {
                        dates.add(current);
                        current = current.plusDays(1);
                    }
                }
                inEvent = false;
            } else if (inEvent) {
                if (trimmed.startsWith("DTSTART")) {
                    dtStart = parseDateFromLine(trimmed);
                } else if (trimmed.startsWith("DTEND")) {
                    dtEnd = parseDateFromLine(trimmed);
                }
            }
        }
        return dates;
    }

    /**
     * Parse a date from an iCal DTSTART/DTEND line.
     * Handles:
     *   DTSTART;VALUE=DATE:20260315          → 2026-03-15 (date only, no TZ conversion)
     *   DTSTART:20260315T140000Z             → Convert UTC to IST, extract date
     *   DTSTART:20260314T200000Z             → 2026-03-15 in IST (date shift!)
     *   DTSTART;TZID=Asia/Kolkata:20260315T120000 → Direct IST, extract date
     */
    private LocalDate parseDateFromLine(String line) {
        try {
            String value = line.substring(line.lastIndexOf(':') + 1).trim();

            // Pure date format (8 chars, no time) — no TZ conversion needed
            if (value.length() == 8) {
                return LocalDate.parse(value, ICAL_DATE_FMT);
            }

            // DateTime with UTC 'Z' suffix — convert to IST
            if (value.endsWith("Z") && value.length() >= 15) {
                String dateStr = value.substring(0, 8);
                String timeStr = value.substring(9, 15);
                LocalDate utcDate = LocalDate.parse(dateStr, ICAL_DATE_FMT);
                LocalTime utcTime = LocalTime.parse(timeStr, DateTimeFormatter.ofPattern("HHmmss"));
                ZonedDateTime utcDt = ZonedDateTime.of(utcDate, utcTime, ZoneOffset.UTC);
                ZonedDateTime istDt = utcDt.withZoneSameInstant(IST);
                return istDt.toLocalDate();
            }

            // DateTime with TZID in the line — check if IST-like
            if (line.contains("TZID=") && value.length() >= 8) {
                return LocalDate.parse(value.substring(0, 8), ICAL_DATE_FMT);
            }

            // Fallback: take first 8 chars
            if (value.length() >= 8) {
                return LocalDate.parse(value.substring(0, 8), ICAL_DATE_FMT);
            }
        } catch (Exception e) {
            log.warn("Failed to parse iCal date from line: {}", line);
        }
        return null;
    }
}
