package com.safar.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
public class AppleTokenVerifier {

    private final RestTemplate restTemplate;

    public AppleTokenVerifier(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public record AppleUserInfo(String appleId, String email, String name) {}

    /**
     * Verify Apple identity token.
     * In production, validate JWT signature against Apple's public keys.
     * For MVP, decode the JWT payload to extract user info.
     */
    public AppleUserInfo verify(String identityToken, String userDisplayName) {
        try {
            // Decode JWT payload (middle segment)
            String[] parts = identityToken.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Invalid Apple identity token format");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            @SuppressWarnings("unchecked")
            Map<String, Object> claims = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(payload, Map.class);

            String sub = (String) claims.get("sub"); // Apple user ID
            String email = (String) claims.get("email");
            String issuer = (String) claims.get("iss");

            if (!"https://appleid.apple.com".equals(issuer)) {
                throw new IllegalArgumentException("Invalid Apple token issuer");
            }

            if (sub == null) {
                throw new IllegalArgumentException("Apple token missing subject (sub)");
            }

            // Apple only sends name on first sign-in, so accept from request
            String name = userDisplayName != null ? userDisplayName : email;

            return new AppleUserInfo(sub, email, name);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Apple token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to verify Apple identity token");
        }
    }
}
