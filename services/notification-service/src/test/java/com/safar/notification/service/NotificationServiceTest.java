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
    @InjectMocks NotificationService notificationService;

    private BookingClient.BookingInfo sampleBooking() {
        return new BookingClient.BookingInfo(
                "bkg-001", "SAF-ABC123",
                "guest-id-1", "host-id-1", "listing-id-1",
                "guest@example.com", "John", "Doe"
        );
    }

    @Test
    void notifyBookingCreated_sendsToGuestAndHost() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());
        when(userClient.getUser("host-id-1")).thenReturn(new UserClient.UserInfo("host@example.com", "Host"));

        notificationService.notifyBookingCreated("bkg-001");

        verify(emailGateway).send(eq("guest@example.com"), contains("Booking Created"), contains("SAF-ABC123"));
        verify(emailGateway).send(eq("host@example.com"), contains("New Booking Received"), contains("SAF-ABC123"));
    }

    @Test
    void notifyBookingCreated_fallsBackToUserService() {
        var bookingNoEmail = new BookingClient.BookingInfo(
                "bkg-002", "SAF-XYZ", "guest-id-2", "host-id-2", "listing-id-2", "", "Jane", "Doe");
        when(bookingClient.getBooking("bkg-002")).thenReturn(bookingNoEmail);
        when(userClient.getUser("guest-id-2")).thenReturn(new UserClient.UserInfo("jane@example.com", "Jane"));
        when(userClient.getUser("host-id-2")).thenReturn(new UserClient.UserInfo("host2@example.com", "Host2"));

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
        when(userClient.getUser("host-id-1")).thenReturn(new UserClient.UserInfo("host@example.com", "Host"));

        notificationService.notifyBookingConfirmed("bkg-001");

        verify(emailGateway).send(eq("guest@example.com"), contains("Confirmed"), contains("SAF-ABC123"));
        verify(emailGateway).send(eq("host@example.com"), contains("Confirmed"), anyString());
    }

    @Test
    void notifyBookingCancelled_sendsToGuestAndHost() {
        when(bookingClient.getBooking("bkg-001")).thenReturn(sampleBooking());
        when(userClient.getUser("host-id-1")).thenReturn(new UserClient.UserInfo("host@example.com", "Host"));

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
