package com.safar.booking.kafka;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyInvoice;
import com.safar.booking.entity.enums.InvoiceStatus;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.repository.TenancyInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
public class TenancyPaymentListener {

    private final PgTenancyRepository tenancyRepository;
    private final TenancyInvoiceRepository invoiceRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RestTemplate restTemplate;
    private final String userServiceUrl;
    private final String listingServiceUrl;

    public TenancyPaymentListener(PgTenancyRepository tenancyRepository,
                                   TenancyInvoiceRepository invoiceRepository,
                                   KafkaTemplate<String, Object> kafkaTemplate,
                                   RestTemplate restTemplate,
                                   @Value("${services.user-service.url}") String userServiceUrl,
                                   @Value("${services.listing-service.url}") String listingServiceUrl) {
        this.tenancyRepository = tenancyRepository;
        this.invoiceRepository = invoiceRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
        this.listingServiceUrl = listingServiceUrl;
    }

    @KafkaListener(topics = "tenancy.subscription.charged", groupId = "booking-service")
    @Transactional
    public void onSubscriptionCharged(Map<String, Object> event) {
        UUID tenancyId = UUID.fromString((String) event.get("tenancyId"));
        String paymentId = (String) event.get("paymentId");

        log.info("Subscription charged for tenancy {}, paymentId: {}", tenancyId, paymentId);

        // Find the latest GENERATED invoice and mark it PAID
        invoiceRepository.findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED)
                .findFirst()
                .ifPresentOrElse(invoice -> {
                    invoice.setStatus(InvoiceStatus.PAID);
                    invoice.setPaidDate(LocalDate.now());
                    invoice.setRazorpayPaymentId(paymentId);
                    invoiceRepository.save(invoice);
                    log.info("Invoice {} marked PAID via subscription charge", invoice.getInvoiceNumber());

                    // Trigger host payout: publish rent.collected event
                    publishRentCollected(tenancyId, invoice);
                }, () -> log.warn("No GENERATED invoice found for tenancy {} to mark PAID", tenancyId));
    }

    private void publishRentCollected(UUID tenancyId, TenancyInvoice invoice) {
        tenancyRepository.findById(tenancyId).ifPresent(tenancy -> {
            // Fetch host's commission rate from user-service subscription
            int commissionBps = fetchHostCommissionBps(tenancy.getListingId());

            kafkaTemplate.send("tenancy.rent.collected", tenancyId.toString(), Map.of(
                    "tenancyId", tenancyId.toString(),
                    "invoiceId", invoice.getId().toString(),
                    "amountPaise", invoice.getGrandTotalPaise(),
                    "hostId", fetchHostId(tenancy.getListingId()).toString(),
                    "commissionBps", commissionBps
            ));
            log.info("Published tenancy.rent.collected for tenancy {}, amount={}, commission={}bps",
                    tenancy.getTenancyRef(), invoice.getGrandTotalPaise(), commissionBps);
        });
    }

    private UUID fetchHostId(UUID listingId) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> listing = restTemplate.getForObject(
                    listingServiceUrl + "/api/v1/listings/" + listingId, Map.class);
            if (listing != null && listing.get("hostId") != null) {
                return UUID.fromString((String) listing.get("hostId"));
            }
        } catch (Exception e) {
            log.warn("Could not fetch listing {} for hostId: {}", listingId, e.getMessage());
        }
        throw new RuntimeException("Could not resolve hostId for listing: " + listingId);
    }

    private int fetchHostCommissionBps(UUID listingId) {
        // Default commission rates by tier (from MEMORY)
        // STARTER=18%, PRO=12%, COMMERCIAL=10%, MEDICAL=8%, AASHRAY=0%
        try {
            UUID hostId = fetchHostId(listingId);
            @SuppressWarnings("unchecked")
            Map<String, Object> sub = restTemplate.getForObject(
                    userServiceUrl + "/api/v1/subscriptions/host/" + hostId, Map.class);
            if (sub != null && sub.get("tier") != null) {
                return switch ((String) sub.get("tier")) {
                    case "STARTER" -> 1800;
                    case "PRO" -> 1200;
                    case "COMMERCIAL" -> 1000;
                    case "MEDICAL" -> 800;
                    case "AASHRAY" -> 0;
                    default -> 1800;
                };
            }
        } catch (Exception e) {
            log.warn("Could not fetch host subscription for listing {}, defaulting to 18%: {}",
                    listingId, e.getMessage());
        }
        return 1800; // Default: STARTER tier
    }

    @KafkaListener(topics = "tenancy.subscription.halted", groupId = "booking-service")
    @Transactional
    public void onSubscriptionHalted(Map<String, Object> event) {
        UUID tenancyId = UUID.fromString((String) event.get("tenancyId"));
        String reason = (String) event.getOrDefault("reason", "Payment failed");

        log.warn("Subscription halted for tenancy {}: {}", tenancyId, reason);

        // Mark latest GENERATED invoice as OVERDUE
        invoiceRepository.findByTenancyIdOrderByBillingYearDescBillingMonthDesc(tenancyId, null)
                .stream()
                .filter(inv -> inv.getStatus() == InvoiceStatus.GENERATED)
                .findFirst()
                .ifPresent(invoice -> {
                    invoice.setStatus(InvoiceStatus.OVERDUE);
                    invoiceRepository.save(invoice);
                    log.info("Invoice {} marked OVERDUE due to subscription halt", invoice.getInvoiceNumber());
                });
    }

    @KafkaListener(topics = "tenancy.subscription.authenticated", groupId = "booking-service")
    @Transactional
    public void onSubscriptionAuthenticated(Map<String, Object> event) {
        UUID tenancyId = UUID.fromString((String) event.get("tenancyId"));
        String subscriptionId = (String) event.get("subscriptionId");

        log.info("Subscription authenticated for tenancy {}: {}", tenancyId, subscriptionId);

        tenancyRepository.findById(tenancyId).ifPresent(tenancy -> {
            tenancy.setSubscriptionStatus("AUTHENTICATED");
            tenancy.setRazorpaySubscriptionId(subscriptionId);
            tenancyRepository.save(tenancy);
        });
    }
}
