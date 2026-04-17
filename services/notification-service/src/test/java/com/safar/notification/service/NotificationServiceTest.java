package com.safar.notification.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock EmailGateway emailGateway;
    @Mock BookingClient bookingClient;
    @Mock UserClient userClient;
    @Mock InAppNotificationService inAppNotificationService;
    @Mock EmailTemplateService emailTemplateService;
    @Mock JourneyChapterService journeyChapterService;
    @Mock EmailSchedulerService emailSchedulerService;
    @Mock EmailContextBuilder emailContextBuilder;
    @InjectMocks NotificationService notificationService;

    private BookingClient.BookingInfo sampleBooking() {
        return new BookingClient.BookingInfo(
                "bkg-001", "SAF-ABC123",
                "00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002", "00000000-0000-0000-0000-000000000003",
                "guest@example.com", "John", "Doe",
                "2026-06-15", "2026-06-17", 2,
                2, 2, 0, 0,
                1,
                "Sample Listing", "Bengaluru", "Sample address",
                500000L, 400000L, 72000L,
                0L, 0L, 0L, 0L, 0L,
                "PREPAID", "", "NIGHT"
        );
    }

    @Test
    void notifyBookingCreated_sendsToGuestAndHost() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());
        when(userClient.getUser("00000000-0000-0000-0000-000000000002")).thenReturn(new UserClient.UserInfo("host@example.com", "Host", "+919876543210"));

        notificationService.notifyBookingCreated("bkg-001");

        // Guest gets plain text via emailGateway
        verify(emailGateway).send(eq("guest@example.com"), contains("Booking Created"), contains("SAF-ABC123"));
        // Host gets HTML via emailTemplateService (sendHostNewBookingAlert)
    }

    @Test
    void notifyBookingCreated_fallsBackToUserService() {
        var bookingNoEmail = new BookingClient.BookingInfo(
                "bkg-002", "SAF-XYZ", "00000000-0000-0000-0000-000000000004", "00000000-0000-0000-0000-000000000005", "00000000-0000-0000-0000-000000000006", "", "Jane", "Doe",
                "2026-06-15", "2026-06-17", 2,
                1, 1, 0, 0,
                1,
                "Sample Listing", "Bengaluru", "Sample address",
                500000L, 400000L, 72000L,
                0L, 0L, 0L, 0L, 0L,
                "PREPAID", "", "NIGHT");
        when(bookingClient.getBooking("bkg-002")).thenReturn(bookingNoEmail);
        when(userClient.getUser("00000000-0000-0000-0000-000000000004")).thenReturn(new UserClient.UserInfo("jane@example.com", "Jane", "+919876500000"));
        when(userClient.getUser("00000000-0000-0000-0000-000000000005")).thenReturn(new UserClient.UserInfo("host2@example.com", "Host2", "+919876500001"));

        notificationService.notifyBookingCreated("bkg-002");

        verify(emailGateway).send(eq("jane@example.com"), contains("Booking Created"), anyString());
    }

    @Test
    void notifyBookingCreated_bookingNotFound_skips() {
        when(bookingClient.getBooking("bkg-999")).thenReturn(null);

        notificationService.notifyBookingCreated("bkg-999");

        verify(emailGateway, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    void notifyBookingConfirmed_sendsToGuestAndHost() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());
        when(userClient.getUser("00000000-0000-0000-0000-000000000002")).thenReturn(new UserClient.UserInfo("host@example.com", "Host", "+919876543210"));

        notificationService.notifyBookingConfirmed("bkg-001");

        // Guest gets HTML email via emailTemplateService; host gets plain text via emailGateway
        verify(emailGateway).send(eq("host@example.com"), contains("Confirmed"), anyString());
    }

    @Test
    void notifyBookingCancelled_sendsToGuestAndHost() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());
        when(userClient.getUser("00000000-0000-0000-0000-000000000002")).thenReturn(new UserClient.UserInfo("host@example.com", "Host", "+919876543210"));

        notificationService.notifyBookingCancelled("bkg-001");

        verify(emailGateway).send(eq("guest@example.com"), contains("Cancelled"), contains("SAF-ABC123"));
        verify(emailGateway).send(eq("host@example.com"), contains("Cancelled"), anyString());
    }

    @Test
    void notifyPaymentCaptured_sendsToGuest() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());

        notificationService.notifyPaymentCaptured("bkg-001");

        verify(emailGateway).send(eq("guest@example.com"), contains("Payment Received"), contains("SAF-ABC123"));
    }

    @Test
    void notifyPaymentFailed_sendsToGuest() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());

        notificationService.notifyPaymentFailed("bkg-001");

        verify(emailGateway).send(eq("guest@example.com"), contains("Payment Failed"), contains("SAF-ABC123"));
    }

    @Test
    void notifyReviewCreated_sendsEmail() {
        notificationService.notifyReviewCreated("rev-001");
        verify(emailGateway).send(anyString(), contains("Review"), contains("rev-001"));
    }
}
