package com.safar.listing.service;

import com.safar.listing.entity.MarketplaceApp;
import com.safar.listing.entity.enums.AppStatus;
import com.safar.listing.repository.MarketplaceAppRepository;
import com.safar.listing.repository.WebhookDeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class WebhookDeliveryServiceTest {

    @Mock
    WebhookDeliveryRepository deliveryRepository;

    @Mock
    MarketplaceAppRepository appRepository;

    @InjectMocks
    WebhookDeliveryService webhookDeliveryService;

    @Test
    void computeHmac_returnsCorrectSignature() throws Exception {
        String payload = "{\"event\":\"booking.created\",\"id\":\"123\"}";
        String secret = "my-webhook-secret-key";

        // Compute expected HMAC using standard Java
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        mac.init(keySpec);
        byte[] expectedBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        String expectedHex = HexFormat.of().formatHex(expectedBytes);

        String actual = webhookDeliveryService.computeHmac(payload, secret);

        assertThat(actual).isEqualTo(expectedHex);
        assertThat(actual).hasSize(64); // SHA-256 produces 32 bytes = 64 hex chars
    }
}
