package com.safar.listing.service;

import com.safar.listing.entity.LocalityPolygon;
import com.safar.listing.entity.enums.PolygonType;
import com.safar.listing.repository.LocalityPolygonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalityPolygonService {

    private final LocalityPolygonRepository polygonRepository;
    private final RestTemplate restTemplate;

    private static final String NOMINATIM_URL = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "SafarPlatform/1.0 (contact@safar.in)";

    /**
     * Fetch polygon from OpenStreetMap Nominatim and store.
     */
    @Transactional
    public LocalityPolygon fetchAndStoreFromOsm(String name, String city, String state) {
        // Check if already exists
        Optional<LocalityPolygon> existing = polygonRepository.findByNameAndCity(name, city);
        if (existing.isPresent()) {
            return existing.get();
        }

        String query = name + ", " + city + ", India";
        String url = NOMINATIM_URL + "?q=" + query.replace(" ", "+")
                + "&format=json&polygon_geojson=1&limit=1";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.GET, request, List.class);

            if (response.getBody() != null && !response.getBody().isEmpty()) {
                Map result = (Map) response.getBody().get(0);
                Map geojson = (Map) result.get("geojson");
                String geoJsonStr = geojson != null ? mapToJson(geojson) : null;

                double lat = Double.parseDouble(result.get("lat").toString());
                double lng = Double.parseDouble(result.get("lon").toString());
                String osmId = result.get("osm_id") != null ? result.get("osm_id").toString() : null;
                String osmType = result.get("osm_type") != null ? result.get("osm_type").toString() : null;

                LocalityPolygon polygon = LocalityPolygon.builder()
                        .name(name)
                        .city(city)
                        .state(state)
                        .osmId(osmType + "/" + osmId)
                        .boundaryGeoJson(geoJsonStr)
                        .centroidLat(lat)
                        .centroidLng(lng)
                        .polygonType(PolygonType.NEIGHBORHOOD)
                        .active(true)
                        .lastUpdatedFromOsm(OffsetDateTime.now())
                        .build();

                LocalityPolygon saved = polygonRepository.save(polygon);
                log.info("Stored polygon for {}, {} (OSM: {})", name, city, osmId);
                return saved;
            }
        } catch (Exception e) {
            log.error("Failed to fetch polygon from OSM for {}, {}: {}", name, city, e.getMessage());
        }

        // Store without polygon (centroid only fallback)
        LocalityPolygon fallback = LocalityPolygon.builder()
                .name(name)
                .city(city)
                .state(state)
                .polygonType(PolygonType.NEIGHBORHOOD)
                .active(true)
                .build();
        return polygonRepository.save(fallback);
    }

    /**
     * Bulk import with rate limiting (1 request per second for Nominatim).
     */
    @Transactional
    public List<LocalityPolygon> bulkImport(List<Map<String, String>> localities) {
        List<LocalityPolygon> results = new ArrayList<>();
        for (Map<String, String> loc : localities) {
            try {
                results.add(fetchAndStoreFromOsm(
                        loc.get("name"), loc.get("city"), loc.getOrDefault("state", null)));
                // Respect Nominatim rate limit: 1 request/second
                TimeUnit.MILLISECONDS.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return results;
    }

    public LocalityPolygon getPolygon(UUID id) {
        return polygonRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Locality polygon not found: " + id));
    }

    public Optional<LocalityPolygon> getPolygonByNameAndCity(String name, String city) {
        return polygonRepository.findByNameAndCity(name, city);
    }

    public List<LocalityPolygon> listByCity(String city) {
        return polygonRepository.findByCityAndActiveTrue(city);
    }

    public List<LocalityPolygon> listAll() {
        return polygonRepository.findByActiveTrue();
    }

    @Transactional
    public LocalityPolygon refreshFromOsm(UUID id) {
        LocalityPolygon polygon = getPolygon(id);
        polygonRepository.delete(polygon);
        return fetchAndStoreFromOsm(polygon.getName(), polygon.getCity(), polygon.getState());
    }

    public Map<String, Long> getStatsByCity() {
        Map<String, Long> stats = new HashMap<>();
        polygonRepository.findByActiveTrue().forEach(p ->
                stats.merge(p.getCity(), 1L, Long::sum));
        return stats;
    }

    private String mapToJson(Map map) {
        // Simplified JSON serialization for GeoJSON
        try {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Object key : map.keySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(key).append("\":");
                Object val = map.get(key);
                if (val instanceof String) {
                    sb.append("\"").append(val).append("\"");
                } else if (val instanceof Map) {
                    sb.append(mapToJson((Map) val));
                } else if (val instanceof List) {
                    sb.append(listToJson((List) val));
                } else {
                    sb.append(val);
                }
                first = false;
            }
            sb.append("}");
            return sb.toString();
        } catch (Exception e) {
            return map.toString();
        }
    }

    private String listToJson(List list) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (Object item : list) {
            if (!first) sb.append(",");
            if (item instanceof Map) {
                sb.append(mapToJson((Map) item));
            } else if (item instanceof List) {
                sb.append(listToJson((List) item));
            } else if (item instanceof String) {
                sb.append("\"").append(item).append("\"");
            } else {
                sb.append(item);
            }
            first = false;
        }
        sb.append("]");
        return sb.toString();
    }
}
