package com.safar.flight.adapter.amadeus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.safar.flight.adapter.*;
import com.safar.flight.dto.CreateFlightBookingRequest;
import com.safar.flight.dto.FlightSearchRequest;
import com.safar.flight.dto.FlightSearchResponse.FlightOffer;
import com.safar.flight.dto.FlightSearchResponse.Segment;
import com.safar.flight.dto.PassengerDTO;
import com.safar.flight.entity.CabinClass;
import com.safar.flight.entity.TripType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Amadeus Self-Service API integration. OAuth2 client credentials.
 * Docs: https://developers.amadeus.com/self-service/apis-docs
 */
@Component
@Slf4j
public class AmadeusFlightAdapter implements FlightProviderAdapter {

    private final WebClient amadeusWebClient;
    private final ObjectMapper objectMapper;

    @Value("${amadeus.client-id:}")
    private String clientId;

    @Value("${amadeus.client-secret:}")
    private String clientSecret;

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private volatile long tokenExpiresAt = 0;

    public AmadeusFlightAdapter(@Qualifier("amadeusWebClient") WebClient amadeusWebClient,
                                ObjectMapper objectMapper) {
        this.amadeusWebClient = amadeusWebClient;
        this.objectMapper = objectMapper;
    }

    @Override public FlightProvider providerType() { return FlightProvider.AMADEUS; }

    @Override
    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank()
                && !"test_client_id".equals(clientId)
                && clientSecret != null && !clientSecret.isBlank();
    }

    @Override public boolean canBook() { return true; }

    // ─── OAuth2 Token Management ────────────────────────────────────

    private String getAccessToken() {
        if (accessToken.get() != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) {
            return accessToken.get();
        }
        return refreshToken();
    }

    private synchronized String refreshToken() {
        if (accessToken.get() != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) {
            return accessToken.get();
        }
        try {
            String response = amadeusWebClient.post()
                    .uri("/v1/security/oauth2/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            String token = node.get("access_token").asText();
            int expiresIn = node.get("expires_in").asInt(1799);

            accessToken.set(token);
            tokenExpiresAt = System.currentTimeMillis() + (expiresIn * 1000L);
            log.info("Amadeus OAuth2 token refreshed, expires in {}s", expiresIn);
            return token;
        } catch (Exception e) {
            log.error("Failed to refresh Amadeus OAuth2 token: {}", e.getMessage());
            throw new FlightProviderException("Amadeus authentication failed", e);
        }
    }

    // ─── Search ─────────────────────────────────────────────────────

    @Override
    public List<FlightOffer> search(FlightSearchRequest request) {
        try {
            ObjectNode body = buildSearchBody(request);
            String token = getAccessToken();

            String responseJson = amadeusWebClient.post()
                    .uri("/v2/shopping/flight-offers")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseSearchResponse(responseJson);
        } catch (WebClientResponseException e) {
            log.error("Amadeus search error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FlightProviderException("Amadeus search failed: " + e.getStatusCode(), e);
        } catch (FlightProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Amadeus search error", e);
            throw new FlightProviderException("Amadeus search failed: " + e.getMessage(), e);
        }
    }

    // ─── Book ───────────────────────────────────────────────────────

    @Override
    public ProviderBookingResult book(String nativeOfferId,
                                      CreateFlightBookingRequest request,
                                      UUID userId) {
        try {
            String token = getAccessToken();
            ObjectNode orderBody = buildOrderBody(nativeOfferId, request);

            String responseJson = amadeusWebClient.post()
                    .uri("/v1/booking/flight-orders")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(orderBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode orderData = objectMapper.readTree(responseJson).path("data");
            String amadeusOrderId = orderData.path("id").asText("");

            JsonNode flightOffersArr = orderData.path("flightOffers");
            JsonNode firstOffer = flightOffersArr.isArray() && !flightOffersArr.isEmpty()
                    ? flightOffersArr.get(0) : objectMapper.createObjectNode();

            JsonNode price = firstOffer.path("price");
            long totalPaise = Math.round(Double.parseDouble(price.path("grandTotal").asText("0")) * 100);
            String currency = price.path("currency").asText("INR");

            long taxPaise = 0;
            JsonNode fees = price.path("fees");
            if (fees.isArray()) {
                for (JsonNode fee : fees) {
                    taxPaise += Math.round(Double.parseDouble(fee.path("amount").asText("0")) * 100);
                }
            }

            JsonNode itineraries = firstOffer.path("itineraries");
            JsonNode firstItinerary = itineraries.isArray() && !itineraries.isEmpty()
                    ? itineraries.get(0) : objectMapper.createObjectNode();
            JsonNode segments = firstItinerary.path("segments");
            JsonNode firstSegment = segments.isArray() && !segments.isEmpty()
                    ? segments.get(0) : objectMapper.createObjectNode();
            JsonNode lastSegment = segments.isArray() && !segments.isEmpty()
                    ? segments.get(segments.size() - 1) : firstSegment;

            String departureCityCode = firstSegment.path("departure").path("iataCode").asText("");
            String arrivalCityCode = lastSegment.path("arrival").path("iataCode").asText("");
            String airline = firstSegment.path("carrierCode").asText("");
            String flightNumber = airline + firstSegment.path("number").asText("");

            boolean isInternational = !isIndianAirport(departureCityCode) || !isIndianAirport(arrivalCityCode);

            int itineraryCount = itineraries.isArray() ? itineraries.size() : 1;
            TripType tripType = itineraryCount > 1 ? TripType.ROUND_TRIP : TripType.ONE_WAY;

            LocalDate departureDate = LocalDate.parse(
                    firstSegment.path("departure").path("at").asText("2026-01-01T00:00").substring(0, 10));

            LocalDate returnDate = null;
            if (tripType == TripType.ROUND_TRIP && itineraries.size() > 1) {
                JsonNode returnItinerary = itineraries.get(1);
                JsonNode returnSegments = returnItinerary.path("segments");
                if (returnSegments.isArray() && !returnSegments.isEmpty()) {
                    returnDate = LocalDate.parse(
                            returnSegments.get(0).path("departure").path("at").asText("").substring(0, 10));
                }
            }

            JsonNode travelerPricings = firstOffer.path("travelerPricings");
            String cabinStr = "ECONOMY";
            if (travelerPricings.isArray() && !travelerPricings.isEmpty()) {
                cabinStr = travelerPricings.get(0).path("fareDetailsBySegment")
                        .path(0).path("cabin").asText("ECONOMY");
            }
            CabinClass cabinClass;
            try { cabinClass = CabinClass.valueOf(cabinStr.toUpperCase()); }
            catch (Exception e) { cabinClass = CabinClass.ECONOMY; }

            return new ProviderBookingResult(
                    amadeusOrderId, "CONFIRMED", totalPaise, taxPaise, currency,
                    departureCityCode, arrivalCityCode, departureDate, returnDate,
                    tripType, cabinClass, airline, flightNumber, isInternational,
                    itineraries.toString()
            );

        } catch (WebClientResponseException e) {
            log.error("Amadeus booking error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FlightProviderException("Amadeus booking failed: " + e.getStatusCode(), e);
        } catch (FlightProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Amadeus booking error", e);
            throw new FlightProviderException("Amadeus booking failed: " + e.getMessage(), e);
        }
    }

    // ─── Cancel ─────────────────────────────────────────────────────

    @Override
    public void cancel(String externalOrderId) {
        if (externalOrderId == null || externalOrderId.isBlank()) {
            return;
        }
        try {
            String token = getAccessToken();
            amadeusWebClient.delete()
                    .uri("/v1/booking/flight-orders/" + externalOrderId)
                    .header("Authorization", "Bearer " + token)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            log.info("Amadeus order cancelled: {}", externalOrderId);
        } catch (WebClientResponseException e) {
            log.error("Amadeus cancel error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FlightProviderException("Amadeus cancel failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Amadeus cancel error: {}", e.getMessage());
            throw new FlightProviderException("Amadeus cancel failed: " + e.getMessage(), e);
        }
    }

    // ─── Build request bodies ───────────────────────────────────────

    private ObjectNode buildSearchBody(FlightSearchRequest request) {
        ObjectNode root = objectMapper.createObjectNode();

        ObjectNode od1 = objectMapper.createObjectNode();
        od1.put("id", "1");
        od1.put("originLocationCode", request.origin());
        od1.put("destinationLocationCode", request.destination());
        ObjectNode depRange1 = od1.putObject("departureDateTimeRange");
        depRange1.put("date", request.departureDate().toString());

        ArrayNode ods = objectMapper.createArrayNode();
        ods.add(od1);

        if (request.returnDate() != null) {
            ObjectNode od2 = objectMapper.createObjectNode();
            od2.put("id", "2");
            od2.put("originLocationCode", request.destination());
            od2.put("destinationLocationCode", request.origin());
            ObjectNode depRange2 = od2.putObject("departureDateTimeRange");
            depRange2.put("date", request.returnDate().toString());
            ods.add(od2);
        }

        root.set("originDestinations", ods);

        ArrayNode travelers = root.putArray("travelers");
        int pax = request.passengers() != null ? request.passengers() : 1;
        for (int i = 1; i <= pax; i++) {
            ObjectNode traveler = travelers.addObject();
            traveler.put("id", String.valueOf(i));
            traveler.put("travelerType", "ADULT");
        }

        ObjectNode searchCriteria = root.putObject("searchCriteria");
        searchCriteria.put("maxFlightOffers", 50);

        ObjectNode flightFilters = searchCriteria.putObject("flightFilters");
        if (request.maxConnections() != null) {
            ObjectNode connectionRestriction = flightFilters.putObject("connectionRestriction");
            connectionRestriction.put("maxNumberOfConnections", request.maxConnections());
        }

        String cabin = request.cabinClass() != null ? request.cabinClass().toUpperCase() : "ECONOMY";
        ArrayNode cabinArr = flightFilters.putArray("cabinRestrictions");
        ObjectNode cabinObj = cabinArr.addObject();
        cabinObj.put("cabin", cabin);
        cabinObj.put("coverage", "MOST_SEGMENTS");
        cabinObj.put("originDestinationIds", "1");

        root.putArray("sources").add("GDS");
        return root;
    }

    private ObjectNode buildOrderBody(String nativeOfferId, CreateFlightBookingRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode data = root.putObject("data");
        data.put("type", "flight-order");

        ArrayNode flightOffers = data.putArray("flightOffers");
        ObjectNode offer = flightOffers.addObject();
        offer.put("type", "flight-offer");
        offer.put("id", nativeOfferId);

        ArrayNode travelers = data.putArray("travelers");
        for (int i = 0; i < request.passengers().size(); i++) {
            PassengerDTO p = request.passengers().get(i);
            ObjectNode t = travelers.addObject();
            t.put("id", String.valueOf(i + 1));
            t.put("dateOfBirth", p.dateOfBirth().toString());

            ObjectNode name = t.putObject("name");
            name.put("firstName", p.firstName().toUpperCase());
            name.put("lastName", p.lastName().toUpperCase());

            ObjectNode gender = t.putObject("gender");
            gender.put("value", p.gender() != null ? p.gender().toUpperCase() : "MALE");

            ObjectNode contact = t.putObject("contact");
            contact.put("emailAddress", request.contactEmail());
            ArrayNode phones = contact.putArray("phones");
            ObjectNode phone = phones.addObject();
            phone.put("countryCallingCode", "91");
            phone.put("number", request.contactPhone() != null
                    ? request.contactPhone().replaceAll("[^0-9]", "") : "");
            phone.put("deviceType", "MOBILE");

            if (p.passportNumber() != null && !p.passportNumber().isBlank()) {
                ArrayNode documents = t.putArray("documents");
                ObjectNode doc = documents.addObject();
                doc.put("documentType", "PASSPORT");
                doc.put("number", p.passportNumber());
                doc.put("expiryDate", p.passportExpiry() != null ? p.passportExpiry().toString() : "");
                doc.put("issuanceCountry", p.nationality() != null ? p.nationality() : "IN");
                doc.put("nationality", p.nationality() != null ? p.nationality() : "IN");
                doc.put("holder", true);
            }
        }

        ArrayNode contacts = data.putArray("contacts");
        ObjectNode mainContact = contacts.addObject();
        mainContact.put("emailAddress", request.contactEmail());
        ArrayNode contactPhones = mainContact.putArray("phones");
        ObjectNode cp = contactPhones.addObject();
        cp.put("countryCallingCode", "91");
        cp.put("number", request.contactPhone() != null
                ? request.contactPhone().replaceAll("[^0-9]", "") : "");
        cp.put("deviceType", "MOBILE");

        return root;
    }

    // ─── Parse search response ──────────────────────────────────────

    private List<FlightOffer> parseSearchResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode dataArr = root.path("data");
            JsonNode dictionaries = root.path("dictionaries");
            JsonNode carriers = dictionaries.path("carriers");
            JsonNode aircraft = dictionaries.path("aircraft");

            List<FlightOffer> offers = new ArrayList<>();
            if (!dataArr.isArray()) return offers;

            for (JsonNode offerNode : dataArr) {
                String nativeId = offerNode.path("id").asText();

                JsonNode price = offerNode.path("price");
                long pricePaise = Math.round(Double.parseDouble(price.path("grandTotal").asText("0")) * 100);
                String currency = price.path("currency").asText("INR");

                JsonNode itineraries = offerNode.path("itineraries");
                if (!itineraries.isArray() || itineraries.isEmpty()) continue;
                JsonNode firstItinerary = itineraries.get(0);
                String duration = firstItinerary.path("duration").asText("");
                JsonNode segmentsNode = firstItinerary.path("segments");
                if (!segmentsNode.isArray() || segmentsNode.isEmpty()) continue;

                JsonNode firstSeg = segmentsNode.get(0);
                JsonNode lastSeg = segmentsNode.get(segmentsNode.size() - 1);

                String carrierCode = firstSeg.path("carrierCode").asText("");
                String airlineName = carriers.path(carrierCode).asText(carrierCode);
                String flightNum = carrierCode + firstSeg.path("number").asText("");
                String departureTime = firstSeg.path("departure").path("at").asText("");
                String arrivalTime = lastSeg.path("arrival").path("at").asText("");
                int stops = segmentsNode.size() - 1;

                String cabinCls = "ECONOMY";
                JsonNode travelerPricings = offerNode.path("travelerPricings");
                if (travelerPricings.isArray() && !travelerPricings.isEmpty()) {
                    cabinCls = travelerPricings.get(0).path("fareDetailsBySegment")
                            .path(0).path("cabin").asText("ECONOMY");
                }

                List<Segment> segmentList = new ArrayList<>();
                for (JsonNode seg : segmentsNode) {
                    String segCarrier = seg.path("carrierCode").asText("");
                    String segAirline = carriers.path(segCarrier).asText(segCarrier);
                    String acCode = seg.path("aircraft").path("code").asText("");
                    String acName = aircraft.path(acCode).asText(acCode);

                    segmentList.add(new Segment(
                            seg.path("id").asText(""),
                            segAirline,
                            segCarrier + seg.path("number").asText(""),
                            seg.path("departure").path("iataCode").asText(""),
                            "",
                            seg.path("arrival").path("iataCode").asText(""),
                            "",
                            seg.path("departure").path("at").asText(""),
                            seg.path("arrival").path("at").asText(""),
                            seg.path("duration").asText(""),
                            acName
                    ));
                }

                String airlineLogo = "https://pics.avs.io/60/60/" + carrierCode + ".png";

                offers.add(new FlightOffer(
                        ProviderOfferId.encode(FlightProvider.AMADEUS, nativeId),
                        airlineName, airlineLogo, flightNum,
                        departureTime, arrivalTime, duration, stops,
                        pricePaise, currency, cabinCls, segmentList,
                        FlightProvider.AMADEUS
                ));
            }
            return offers;
        } catch (Exception e) {
            log.error("Error parsing Amadeus search response", e);
            throw new FlightProviderException("Failed to parse Amadeus search results", e);
        }
    }

    private static final Set<String> INDIAN_AIRPORTS = Set.of(
            "DEL", "BOM", "BLR", "HYD", "MAA", "CCU", "GOI", "COK", "JAI", "AMD",
            "PNQ", "LKO", "GAU", "SXR", "IXC", "PAT", "BBI", "NAG", "VNS", "IXB",
            "TRV", "VTZ", "IDR", "STV", "UDR", "RPR", "IXR", "CJB", "IXM", "TRZ",
            "IXA", "IMF", "DIB", "DED", "IXJ", "VGA", "HBX", "IXL", "AGR", "BHO",
            "DBR", "GYA", "BDQ", "JDH", "CNN", "MYQ", "IXE", "DHM", "SAG", "IXD",
            "GOP", "KNU", "TIR", "IXZ", "AYJ"
    );

    private boolean isIndianAirport(String iataCode) {
        return INDIAN_AIRPORTS.contains(iataCode);
    }
}
