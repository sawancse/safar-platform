package com.safar.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleTokenVerifier {

    private final RestTemplate restTemplate;

    private static final String GOOGLE_TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token={idToken}";

    public record GoogleUserInfo(String googleId, String email, String name, String pictureUrl) {}

    public GoogleUserInfo verify(String idToken) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    GOOGLE_TOKENINFO_URL, Map.class, idToken);

            if (response == null) {
                throw new IllegalArgumentException("Invalid Google ID token");
            }

            String googleId = (String) response.get("sub");
            String email = (String) response.get("email");
            String name = (String) response.get("name");
            String pictureUrl = (String) response.get("picture");

            if (googleId == null || email == null) {
                throw new IllegalArgumentException("Google token missing required fields (sub, email)");
            }

            return new GoogleUserInfo(googleId, email, name, pictureUrl);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Google token verification failed: {}", e.getMessage());
            throw new IllegalArgumentException("Failed to verify Google ID token");
        }
    }
}
