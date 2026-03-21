package com.safar.payment.service;

import com.safar.payment.entity.HostInvoice;
import com.safar.payment.entity.enums.InvoiceStatus;
import com.safar.payment.entity.enums.SubscriptionTier;
import com.safar.payment.repository.HostInvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock RazorpayGateway razorpayGateway;
    @Mock HostInvoiceRepository invoiceRepository;
    @Mock InvoiceNumberGenerator invoiceNumberGenerator;

    @InjectMocks SubscriptionService subscriptionService;

    private final UUID hostId = UUID.randomUUID();

    void injectWebhookSecret() {
        ReflectionTestUtils.setField(subscriptionService, "webhookSecret", "test_webhook_secret");
    }

    @Test
    void createSubscription_starter_calculatesCorrectGst() throws Exception {
        when(razorpayGateway.createSubscription(eq("STARTER"), anyLong())).thenReturn("sub_test_starter");
        when(invoiceNumberGenerator.next()).thenReturn("SAF-INV-000001");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HostInvoice invoice = subscriptionService.createSubscription(hostId, "STARTER");

        // STARTER = ₹999 = 99900 paise; GST = 18% → 17982 paise
        assertThat(invoice.getAmountPaise()).isEqualTo(99900L);
        assertThat(invoice.getGstAmountPaise()).isEqualTo(Math.round(99900 * 0.18));
        assertThat(invoice.getTotalPaise()).isEqualTo(99900L + Math.round(99900 * 0.18));
        assertThat(invoice.getTier()).isEqualTo(SubscriptionTier.STARTER);
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.ISSUED);
        assertThat(invoice.getInvoiceNumber()).isEqualTo("SAF-INV-000001");
    }

    @Test
    void createSubscription_pro_setsCorrectTier() throws Exception {
        when(razorpayGateway.createSubscription(eq("PRO"), anyLong())).thenReturn("sub_test_pro");
        when(invoiceNumberGenerator.next()).thenReturn("SAF-INV-000002");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HostInvoice invoice = subscriptionService.createSubscription(hostId, "PRO");

        assertThat(invoice.getTier()).isEqualTo(SubscriptionTier.PRO);
        assertThat(invoice.getAmountPaise()).isEqualTo(249900L);
    }

    @Test
    void createSubscription_commercial_setsCorrectAmount() throws Exception {
        when(razorpayGateway.createSubscription(eq("COMMERCIAL"), anyLong())).thenReturn("sub_test_comm");
        when(invoiceNumberGenerator.next()).thenReturn("SAF-INV-000003");
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        HostInvoice invoice = subscriptionService.createSubscription(hostId, "COMMERCIAL");

        assertThat(invoice.getAmountPaise()).isEqualTo(399900L);
        assertThat(invoice.getTier()).isEqualTo(SubscriptionTier.COMMERCIAL);
    }

    @Test
    void handleWebhook_validSignature_marksInvoicePaid() {
        injectWebhookSecret();
        String subId = "sub_test123";
        String payload = """
                {"event":"subscription.charged","payload":{"subscription":{"entity":{"id":"%s"}}}}
                """.formatted(subId).strip();

        HostInvoice invoice = HostInvoice.builder()
                .id(UUID.randomUUID()).hostId(hostId)
                .razorpaySubId(subId)
                .invoiceNumber("SAF-INV-000001")
                .tier(SubscriptionTier.STARTER)
                .amountPaise(99900L).gstAmountPaise(17982L).totalPaise(117882L)
                .status(InvoiceStatus.ISSUED)
                .billingPeriodStart(LocalDate.now()).billingPeriodEnd(LocalDate.now().plusMonths(1))
                .build();

        when(razorpayGateway.verifyWebhookSignature(payload, "valid_sig", "test_webhook_secret"))
                .thenReturn(true);
        when(invoiceRepository.findByRazorpaySubId(subId)).thenReturn(Optional.of(invoice));
        when(invoiceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        subscriptionService.handleWebhook(payload, "valid_sig");

        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.PAID);
        verify(invoiceRepository).save(invoice);
    }

    @Test
    void handleWebhook_invalidSignature_throwsSecurity() {
        injectWebhookSecret();
        String payload = "{\"event\":\"subscription.charged\"}";

        when(razorpayGateway.verifyWebhookSignature(payload, "bad_sig", "test_webhook_secret"))
                .thenReturn(false);

        assertThatThrownBy(() -> subscriptionService.handleWebhook(payload, "bad_sig"))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Invalid webhook signature");
    }
}
