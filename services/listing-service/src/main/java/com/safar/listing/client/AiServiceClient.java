package com.safar.listing.client;

import com.safar.listing.entity.ListingDraft;
import com.safar.listing.entity.ScoutLead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.ai-service.url:http://localhost:8090}")
    private String aiServiceUrl;

    /** Generate scout leads from the Python AI service */
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "aiService", fallbackMethod = "generateLeadsFallback")
    @Retry(name = "aiService")
    public List<ScoutLeadDto> generateLeads(String city, String propertyType, int limit) {
        String url = UriComponentsBuilder.fromHttpUrl(aiServiceUrl)
                .path("/api/v1/ai/scout/generate-leads")
                .queryParam("city", city)
                .queryParam("property_type", propertyType)
                .queryParam("limit", limit)
                .toUriString();
        Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);
        if (response == null) return List.of();
        List<Map<String, Object>> leads = (List<Map<String, Object>>) response.get("leads");
        return leads.stream().map(l -> new ScoutLeadDto(
                (String) l.get("address"),
                (String) l.get("city"),
                ((Number) l.get("estimated_monthly_income_paise")).longValue(),
                (String) l.get("outreach_message_en"),
                (String) l.get("outreach_message_hi")
        )).toList();
    }

    public List<ScoutLeadDto> generateLeadsFallback(String city, String propertyType, int limit, Throwable t) {
        log.warn("AI service unavailable, returning empty leads: {}", t.getMessage());
        return List.of();
    }

    /** Generate a listing draft from the Python AI service */
    @SuppressWarnings("unchecked")
    @CircuitBreaker(name = "aiService", fallbackMethod = "generateDraftFallback")
    @Retry(name = "aiService")
    public GeneratedDraftDto generateDraft(String address, String city,
                                            String propertyType, int bedrooms) {
        String url = UriComponentsBuilder.fromHttpUrl(aiServiceUrl)
                .path("/api/v1/ai/listing/generate-draft")
                .queryParam("address", address)
                .queryParam("city", city)
                .queryParam("property_type", propertyType)
                .queryParam("bedrooms", bedrooms)
                .toUriString();
        Map<String, Object> r = restTemplate.postForObject(url, null, Map.class);
        if (r == null) return GeneratedDraftDto.fallback(city, propertyType);
        List<String> amenities = (List<String>) r.get("suggested_amenities");
        return new GeneratedDraftDto(
                (String) r.get("title"),
                (String) r.get("description"),
                amenities != null ? String.join(",", amenities) : "wifi,ac",
                ((Number) r.get("suggested_price_paise")).longValue()
        );
    }

    public GeneratedDraftDto generateDraftFallback(String address, String city,
                                                    String propertyType, int bedrooms, Throwable t) {
        log.warn("AI service unavailable for draft generation: {}", t.getMessage());
        return GeneratedDraftDto.fallback(city, propertyType);
    }

    public record ScoutLeadDto(String address, String city, long estimatedMonthlyIncomePaise,
                                String outreachMessageEn, String outreachMessageHi) {}

    public record GeneratedDraftDto(String title, String description,
                                     String amenities, long suggestedPricePaise) {
        static GeneratedDraftDto fallback(String city, String type) {
            return new GeneratedDraftDto(
                    "New " + type + " in " + city,
                    "A comfortable property in " + city + ".",
                    "wifi,ac",
                    200000L
            );
        }
    }
}
