package com.safar.payment.service;

import com.safar.payment.entity.HostInvoice;
import com.safar.payment.entity.enums.InvoiceStatus;
import com.safar.payment.entity.enums.SubscriptionTier;
import com.safar.payment.repository.HostInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final RazorpayGateway razorpayGateway;
    private final HostInvoiceRepository invoiceRepository;
    private final InvoiceNumberGenerator invoiceNumberGenerator;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    private static final Map<String, Long> TIER_PRICE = Map.of(
            "STARTER",     99900L,   // ₹999
            "PRO",        249900L,   // ₹2,499
            "COMMERCIAL", 399900L    // ₹3,999
    );

    @Transactional
    public HostInvoice createSubscription(UUID hostId, String tier) throws Exception {
        long amountPaise = TIER_PRICE.get(tier);
        long gstPaise = Math.round(amountPaise * 0.18);
        long totalPaise = amountPaise + gstPaise;

        String razorpaySubId = razorpayGateway.createSubscription(tier, totalPaise);

        HostInvoice invoice = HostInvoice.builder()
                .hostId(hostId)
                .razorpaySubId(razorpaySubId)
                .invoiceNumber(invoiceNumberGenerator.next())
                .tier(SubscriptionTier.valueOf(tier))
                .amountPaise(amountPaise)
                .gstAmountPaise(gstPaise)
                .totalPaise(totalPaise)
                .status(InvoiceStatus.ISSUED)
                .billingPeriodStart(LocalDate.now())
                .billingPeriodEnd(LocalDate.now().plusMonths(1))
                .build();

        return invoiceRepository.save(invoice);
    }

    public List<HostInvoice> getInvoices(UUID hostId) {
        return invoiceRepository.findByHostId(hostId);
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!razorpayGateway.verifyWebhookSignature(payload, signature, webhookSecret)) {
            throw new SecurityException("Invalid webhook signature");
        }

        // Parse event type from payload (simple JSON parsing)
        String eventType = extractEventType(payload);
        if ("subscription.charged".equals(eventType)) {
            String subId = extractSubscriptionId(payload);
            invoiceRepository.findByRazorpaySubId(subId).ifPresent(inv -> {
                inv.setStatus(InvoiceStatus.PAID);
                invoiceRepository.save(inv);
                log.info("Invoice {} marked PAID for sub {}", inv.getInvoiceNumber(), subId);
            });
        }
    }

    private String extractEventType(String payload) {
        // Simple extraction — production would use ObjectMapper
        int start = payload.indexOf("\"event\"") + 9;
        int end = payload.indexOf("\"", start + 1);
        if (start > 8 && end > start) {
            return payload.substring(start, end);
        }
        return "";
    }

    private String extractSubscriptionId(String payload) {
        // Simplified for MVP — production would use ObjectMapper
        int idx = payload.indexOf("\"subscription\"");
        if (idx < 0) return "";
        int idIdx = payload.indexOf("\"id\"", idx);
        if (idIdx < 0) return "";
        int start = payload.indexOf("\"", idIdx + 5);
        int end = payload.indexOf("\"", start + 1);
        if (start >= 0 && end > start) {
            return payload.substring(start + 1, end);
        }
        return "";
    }
}
