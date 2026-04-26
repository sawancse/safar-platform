package com.safar.services.service;

import com.safar.services.entity.ChefBooking;
import com.safar.services.entity.EventBooking;
import com.safar.services.entity.enums.ChefBookingStatus;
import com.safar.services.entity.enums.EventBookingStatus;
import com.safar.services.repository.ChefBookingRepository;
import com.safar.services.repository.EventBookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final ChefBookingRepository bookingRepo;
    private final EventBookingRepository eventRepo;
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generateInvoiceNumber() {
        return "SINV-" + System.currentTimeMillis() / 1000 + "-" + String.format("%04d", RANDOM.nextInt(9999));
    }

    @Transactional
    public Map<String, Object> generateBookingInvoice(UUID bookingId) {
        ChefBooking b = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new IllegalArgumentException("Booking not found"));

        if (b.getStatus() != ChefBookingStatus.COMPLETED && b.getStatus() != ChefBookingStatus.CONFIRMED) {
            throw new IllegalArgumentException("Invoice only available for confirmed/completed bookings");
        }

        if (b.getInvoiceNumber() == null) {
            b.setInvoiceNumber(generateInvoiceNumber());
            bookingRepo.save(b);
        }

        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNumber", b.getInvoiceNumber());
        invoice.put("type", "CHEF_BOOKING");
        invoice.put("bookingRef", b.getBookingRef());
        invoice.put("customerName", b.getCustomerName());
        invoice.put("chefName", b.getChefName());
        invoice.put("serviceDate", b.getServiceDate() != null ? b.getServiceDate().toString() : "");
        invoice.put("serviceTime", b.getServiceTime());
        invoice.put("mealType", b.getMealType() != null ? b.getMealType().name() : "");
        invoice.put("guestsCount", b.getGuestsCount());
        invoice.put("menuName", b.getMenuName());
        invoice.put("address", b.getAddress());
        invoice.put("city", b.getCity());

        // Line items
        invoice.put("subtotalPaise", b.getTotalAmountPaise());
        invoice.put("platformFeePaise", b.getPlatformFeePaise());
        invoice.put("totalPaise", b.getTotalAmountPaise());
        invoice.put("advancePaidPaise", b.getAdvanceAmountPaise());
        invoice.put("balanceDuePaise", b.getBalanceAmountPaise());
        invoice.put("paymentStatus", b.getPaymentStatus());

        invoice.put("status", b.getStatus().name());
        invoice.put("createdAt", b.getCreatedAt() != null ? b.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "");
        invoice.put("company", "Safar India Pvt. Ltd.");
        invoice.put("gstin", "36AADCS1234P1ZL");

        return invoice;
    }

    @Transactional
    public Map<String, Object> generateEventInvoice(UUID eventId) {
        EventBooking e = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        if (e.getStatus() != EventBookingStatus.COMPLETED && e.getStatus() != EventBookingStatus.CONFIRMED
                && e.getStatus() != EventBookingStatus.ADVANCE_PAID) {
            throw new IllegalArgumentException("Invoice only available for confirmed/completed events");
        }

        if (e.getInvoiceNumber() == null) {
            e.setInvoiceNumber(generateInvoiceNumber());
            eventRepo.save(e);
        }

        Map<String, Object> invoice = new LinkedHashMap<>();
        invoice.put("invoiceNumber", e.getInvoiceNumber());
        invoice.put("type", "EVENT_BOOKING");
        invoice.put("bookingRef", e.getBookingRef());
        invoice.put("customerName", e.getCustomerName());
        invoice.put("chefName", e.getChefName());
        invoice.put("eventType", e.getEventType());
        invoice.put("eventDate", e.getEventDate() != null ? e.getEventDate().toString() : "");
        invoice.put("eventTime", e.getEventTime());
        invoice.put("guestCount", e.getGuestCount());
        invoice.put("venueAddress", e.getVenueAddress());
        invoice.put("city", e.getCity());

        invoice.put("foodPaise", e.getTotalFoodPaise());
        invoice.put("decorationPaise", e.getDecorationPaise());
        invoice.put("cakePaise", e.getCakePaise());
        invoice.put("staffPaise", e.getStaffPaise());
        invoice.put("otherAddonsPaise", e.getOtherAddonsPaise());
        invoice.put("subtotalPaise", e.getTotalAmountPaise());
        invoice.put("platformFeePaise", e.getPlatformFeePaise());
        invoice.put("totalPaise", e.getTotalAmountPaise());
        invoice.put("advancePaidPaise", e.getAdvanceAmountPaise());
        invoice.put("balanceDuePaise", e.getBalanceAmountPaise());

        invoice.put("status", e.getStatus().name());
        invoice.put("createdAt", e.getCreatedAt() != null ? e.getCreatedAt().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : "");
        invoice.put("company", "Safar India Pvt. Ltd.");
        invoice.put("gstin", "36AADCS1234P1ZL");

        return invoice;
    }
}
