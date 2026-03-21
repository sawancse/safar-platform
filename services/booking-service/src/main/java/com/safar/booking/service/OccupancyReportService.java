package com.safar.booking.service;

import com.safar.booking.dto.OccupancyReportDto;
import com.safar.booking.dto.OccupancyReportDto.ListingOccupancy;
import com.safar.booking.dto.OccupancyReportDto.MonthlyBreakdown;
import com.safar.booking.entity.Booking;
import com.safar.booking.entity.enums.BookingStatus;
import com.safar.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OccupancyReportService {

    private final BookingRepository bookingRepo;
    private final ListingServiceClient listingClient;

    private static final List<BookingStatus> COUNTED_STATUSES = List.of(
            BookingStatus.CONFIRMED, BookingStatus.CHECKED_IN, BookingStatus.COMPLETED
    );

    public OccupancyReportDto getOccupancyReport(UUID hostId, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt = to.plusDays(1).atStartOfDay();

        // Get all bookings for this host in the date range
        List<Booking> allBookings = bookingRepo.findByHostIdAndCheckInBetween(hostId, fromDt, toDt);

        // Filter to counted statuses
        List<Booking> bookings = allBookings.stream()
                .filter(b -> COUNTED_STATUSES.contains(b.getStatus()))
                .toList();

        long totalDaysInRange = ChronoUnit.DAYS.between(from, to) + 1;

        // Group by listing
        Map<UUID, List<Booking>> byListing = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getListingId));

        List<ListingOccupancy> listingStats = new ArrayList<>();
        long totalRevenue = 0;
        int totalNights = 0;
        int totalBookingCount = bookings.size();

        for (Map.Entry<UUID, List<Booking>> entry : byListing.entrySet()) {
            UUID listingId = entry.getKey();
            List<Booking> listingBookings = entry.getValue();

            String title = listingClient.getListingTitle(listingId);

            int bookedNights = listingBookings.stream()
                    .mapToInt(b -> b.getNights() != null ? b.getNights() : 0)
                    .sum();
            long revenue = listingBookings.stream()
                    .mapToLong(Booking::getTotalAmountPaise)
                    .sum();

            double occupancy = totalDaysInRange > 0
                    ? Math.min((double) bookedNights / totalDaysInRange * 100.0, 100.0)
                    : 0.0;

            listingStats.add(new ListingOccupancy(
                    listingId, title,
                    Math.round(occupancy * 100.0) / 100.0,
                    revenue, bookedNights,
                    (int) totalDaysInRange,
                    listingBookings.size()
            ));

            totalRevenue += revenue;
            totalNights += bookedNights;
        }

        // Overall metrics
        int totalListings = Math.max(byListing.size(), 1);
        long totalAvailableNights = totalDaysInRange * totalListings;
        double overallOccupancy = totalAvailableNights > 0
                ? Math.min((double) totalNights / totalAvailableNights * 100.0, 100.0)
                : 0.0;
        long adr = totalNights > 0 ? totalRevenue / totalNights : 0;
        long revpar = totalAvailableNights > 0 ? totalRevenue / totalAvailableNights : 0;

        // Monthly breakdown
        List<MonthlyBreakdown> monthly = buildMonthlyBreakdown(bookings, from, to);

        return new OccupancyReportDto(
                Math.round(overallOccupancy * 100.0) / 100.0,
                adr, revpar, totalRevenue,
                totalBookingCount, totalNights,
                listingStats, monthly
        );
    }

    private List<MonthlyBreakdown> buildMonthlyBreakdown(List<Booking> bookings, LocalDate from, LocalDate to) {
        List<MonthlyBreakdown> result = new ArrayList<>();
        YearMonth start = YearMonth.from(from);
        YearMonth end = YearMonth.from(to);

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        YearMonth current = start;
        while (!current.isAfter(end)) {
            final YearMonth ym = current;
            LocalDate monthStart = ym.atDay(1);
            LocalDate monthEnd = ym.atEndOfMonth();

            // Filter bookings that fall in this month
            List<Booking> monthBookings = bookings.stream()
                    .filter(b -> {
                        LocalDate checkIn = b.getCheckIn().toLocalDate();
                        return !checkIn.isBefore(monthStart) && !checkIn.isAfter(monthEnd);
                    })
                    .toList();

            int nights = monthBookings.stream()
                    .mapToInt(b -> b.getNights() != null ? b.getNights() : 0)
                    .sum();
            long revenue = monthBookings.stream()
                    .mapToLong(Booking::getTotalAmountPaise)
                    .sum();

            long daysInMonth = monthEnd.getDayOfMonth();
            double occupancy = daysInMonth > 0
                    ? Math.min((double) nights / daysInMonth * 100.0, 100.0)
                    : 0.0;

            result.add(new MonthlyBreakdown(
                    ym.format(monthFmt),
                    Math.round(occupancy * 100.0) / 100.0,
                    revenue,
                    monthBookings.size()
            ));

            current = current.plusMonths(1);
        }
        return result;
    }
}
