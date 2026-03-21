package com.safar.listing.service;

import com.safar.listing.entity.MarketplaceApp;
import com.safar.listing.entity.WebhookDelivery;
import com.safar.listing.entity.enums.WebhookDeliveryStatus;
import com.safar.listing.repository.MarketplaceAppRepository;
import com.safar.listing.repository.WebhookDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDeliveryService {

    private final WebhookDeliveryRepository deliveryRepository;
    private final MarketplaceAppRepository appRepository;

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Transactional
    public WebhookDelivery dispatchWebhook(UUID appId, String eventType, String payload) {
        MarketplaceApp app = appRepository.findById(appId)
                .orElseThrow(() -> new NoSuchElementException("App not found: " + appId));

        String signature = null;
        if (app.getWebhookSecret() != null && !app.getWebhookSecret().isBlank()) {
            signature = computeHmac(payload, app.getWebhookSecret());
        }

        WebhookDelivery delivery = WebhookDelivery.builder()
                .appId(appId)
                .eventType(eventType)
                .payload(payload)
                .status(WebhookDeliveryStatus.PENDING)
                .attemptCount(0)
                .build();

        WebhookDelivery saved = deliveryRepository.save(delivery);
        log.info("Webhook delivery {} created for app {} event={} signature={}",
                saved.getId(), appId, eventType, signature);
        return saved;
    }

    public String computeHmac(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(keySpec);
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }
}
