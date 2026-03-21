package com.safar.listing.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate;

    /**
     * Geocode an Indian address using OpenStreetMap Nominatim (free, no API key).
     * Returns BigDecimal[]{lat, lng} or null if not found.
     */
    @SuppressWarnings("unchecked")
    public BigDecimal[] geocode(String pincode, String city, String state) {
        try {
            String query = URLEncoder.encode(
                    pincode + " " + city + " " + state + " India",
                    StandardCharsets.UTF_8);
            String url = "https://nominatim.openstreetmap.org/search?format=json&countrycodes=in&limit=1&q=" + query;

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "SafarPlatform/1.0");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, entity, List.class);
            List<Map<String, Object>> results = response.getBody();

            if (results == null || results.isEmpty()) {
                log.debug("Nominatim returned no results for: {} {} {}", pincode, city, state);
                return null;
            }

            Map<String, Object> first = results.get(0);
            BigDecimal lat = new BigDecimal(first.get("lat").toString());
            BigDecimal lng = new BigDecimal(first.get("lon").toString());

            log.info("Geocoded {} {} {} -> [{}, {}]", pincode, city, state, lat, lng);
            return new BigDecimal[]{lat, lng};
        } catch (Exception e) {
            log.warn("Geocoding failed for {} {} {}: {}", pincode, city, state, e.getMessage());
            return null;
        }
    }
}
