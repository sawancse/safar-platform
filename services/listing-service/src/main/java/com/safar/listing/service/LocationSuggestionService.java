package com.safar.listing.service;

import com.safar.listing.entity.LocationSuggestion;
import com.safar.listing.repository.LocationSuggestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationSuggestionService {

    private final LocationSuggestionRepository locationRepo;

    /**
     * Returns suggestions grouped by type for autocomplete.
     * Example: { "CITY": [...], "LOCALITY": [...], "IT_PARK": [...] }
     */
    public Map<String, List<LocationSuggestionDto>> suggest(String query) {
        if (query == null || query.length() < 2) {
            return Map.of();
        }

        List<LocationSuggestion> results = locationRepo.findByNamePrefix(query);

        // Group by type, limit to top 5 per category
        return results.stream()
                .map(this::toDto)
                .collect(Collectors.groupingBy(
                        LocationSuggestionDto::type,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream().limit(5).toList()
                        )
                ));
    }

    public List<LocationSuggestionDto> getByCity(String city) {
        return locationRepo.findByCityOrderByPopularityScoreDesc(city)
                .stream().map(this::toDto).toList();
    }

    private LocationSuggestionDto toDto(LocationSuggestion ls) {
        return new LocationSuggestionDto(
                ls.getId().toString(), ls.getName(), ls.getDisplayName(),
                ls.getType(), ls.getCity(), ls.getState(),
                ls.getLat().doubleValue(), ls.getLng().doubleValue(),
                ls.getDefaultRadiusKm()
        );
    }

    public record LocationSuggestionDto(
            String id, String name, String displayName,
            String type, String city, String state,
            Double lat, Double lng, Double defaultRadiusKm
    ) {}
}
