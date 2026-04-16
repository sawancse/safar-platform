package com.safar.flight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.safar.flight.dto.*;
import com.safar.flight.entity.*;
import com.safar.flight.repository.FlightBookingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Amadeus Self-Service API integration for flight search, booking, and cancellation.
 *
 * Auth: OAuth2 Client Credentials → POST /v1/security/oauth2/token
 * Search: POST /v2/shopping/flight-offers
 * Price: POST /v1/shopping/flight-offers/pricing
 * Book: POST /v1/booking/flight-orders
 * Cancel: DELETE /v1/booking/flight-orders/{orderId}
 *
 * Docs: https://developers.amadeus.com/self-service/apis-docs
 */
@Service
@Slf4j
public class AmadeusFlightService {

    private final WebClient amadeusWebClient;
    private final FlightBookingRepository bookingRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${amadeus.client-id}")
    private String clientId;

    @Value("${amadeus.client-secret}")
    private String clientSecret;

    // Cached OAuth2 token
    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private volatile long tokenExpiresAt = 0;

    public AmadeusFlightService(WebClient amadeusWebClient,
                                 FlightBookingRepository bookingRepository,
                                 KafkaTemplate<String, String> kafkaTemplate,
                                 ObjectMapper objectMapper) {
        this.amadeusWebClient = amadeusWebClient;
        this.bookingRepository = bookingRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // ─── OAuth2 Token Management ────────────────────────────────────

    private String getAccessToken() {
        if (accessToken.get() != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) {
            return accessToken.get();
        }
        return refreshToken();
    }

    private synchronized String refreshToken() {
        // Double-check after acquiring lock
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
            throw new IllegalStateException("Amadeus authentication failed");
        }
    }

    // ─── Flight Search ──────────────────────────────────────────────

    /**
     * Search flights via Amadeus Flight Offers Search API.
     * POST /v2/shopping/flight-offers
     */
    public FlightSearchResponse searchFlights(FlightSearchRequest request) {
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
            throw new IllegalStateException("Flight search failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Flight search error", e);
            throw new IllegalStateException("Flight search failed: " + e.getMessage());
        }
    }

    // ─── Flight Booking ─────────────────────────────────────────────

    /**
     * Create a flight booking via Amadeus Flight Orders API.
     * POST /v1/booking/flight-orders
     */
    @Transactional
    public FlightBookingResponse createBooking(UUID userId, CreateFlightBookingRequest request) {
        try {
            // Step 1: Confirm pricing
            String token = getAccessToken();
            ObjectNode priceBody = objectMapper.createObjectNode();
            ObjectNode priceData = priceBody.putObject("data");
            priceData.put("type", "flight-offers-pricing");
            ArrayNode flightOffers = priceData.putArray("flightOffers");
            // The offerId from search is the full offer JSON stored client-side
            // For Amadeus, we pass the offer object to pricing endpoint
            ObjectNode offerNode = objectMapper.createObjectNode();
            offerNode.put("type", "flight-offer");
            offerNode.put("id", request.offerId());
            flightOffers.add(offerNode);

            // Step 2: Create order
            ObjectNode orderBody = buildOrderBody(request);

            String responseJson = amadeusWebClient.post()
                    .uri("/v1/booking/flight-orders")
                    .header("Authorization", "Bearer " + token)
                    .bodyValue(orderBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode orderData = objectMapper.readTree(responseJson).path("data");
            String amadeusOrderId = orderData.path("id").asText("");
            String bookingRef = generateBookingRef();

            // Extract flight details
            JsonNode flightOffersArr = orderData.path("flightOffers");
            JsonNode firstOffer = flightOffersArr.isArray() && !flightOffersArr.isEmpty()
                    ? flightOffersArr.get(0) : objectMapper.createObjectNode();

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

            // Price
            JsonNode price = firstOffer.path("price");
            String totalAmount = price.path("grandTotal").asText("0");
            long totalPaise = Math.round(Double.parseDouble(totalAmount) * 100);
            String currency = price.path("currency").asText("INR");

            // Tax from price breakdown
            long taxPaise = 0;
            JsonNode fees = price.path("fees");
            if (fees.isArray()) {
                for (JsonNode fee : fees) {
                    taxPaise += Math.round(Double.parseDouble(fee.path("amount").asText("0")) * 100);
                }
            }

            long platformFeePaise = Math.round(totalPaise * 0.02);

            // International check
            boolean isInternational = false;
            if (segments.isArray() && !segments.isEmpty()) {
                // Simple heuristic: if any segment crosses country boundaries
                // Amadeus doesn't have country code in segment, so check if origin/dest differ from Indian airports
                String depCountry = firstSegment.path("departure").path("iataCode").asText("");
                isInternational = !isIndianAirport(departureCityCode) || !isIndianAirport(arrivalCityCode);
            }

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

            // Parse cabin class from first segment
            JsonNode travelerPricings = firstOffer.path("travelerPricings");
            String cabinStr = "ECONOMY";
            if (travelerPricings.isArray() && !travelerPricings.isEmpty()) {
                cabinStr = travelerPricings.get(0).path("fareDetailsBySegment")
                        .path(0).path("cabin").asText("ECONOMY");
            }
            CabinClass cabinClass;
            try { cabinClass = CabinClass.valueOf(cabinStr.toUpperCase()); }
            catch (Exception e) { cabinClass = CabinClass.ECONOMY; }

            FlightBooking booking = FlightBooking.builder()
                    .userId(userId)
                    .bookingRef(bookingRef)
                    .duffelOrderId(amadeusOrderId) // reusing field for Amadeus order ID
                    .status(FlightBookingStatus.PENDING_PAYMENT)
                    .tripType(tripType)
                    .cabinClass(cabinClass)
                    .departureCity(departureCityCode)
                    .departureCityCode(departureCityCode)
                    .arrivalCity(arrivalCityCode)
                    .arrivalCityCode(arrivalCityCode)
                    .departureDate(departureDate)
                    .returnDate(returnDate)
                    .airline(airline)
                    .flightNumber(flightNumber)
                    .isInternational(isInternational)
                    .passengersJson(objectMapper.writeValueAsString(request.passengers()))
                    .slicesJson(itineraries.toString())
                    .totalAmountPaise(totalPaise)
                    .taxPaise(taxPaise)
                    .platformFeePaise(platformFeePaise)
                    .currency(currency)
                    .contactEmail(request.contactEmail())
                    .contactPhone(request.contactPhone())
                    .build();

            booking = bookingRepository.save(booking);
            log.info("Flight booking created: {} for user {} (Amadeus orderId={})", bookingRef, userId, amadeusOrderId);
            publishEvent("flight.booking.created", booking);
            return toResponse(booking);

        } catch (WebClientResponseException e) {
            log.error("Amadeus booking error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new IllegalStateException("Flight booking failed: " + e.getMessage());
        } catch (Exception e) {
            log.error("Flight booking error", e);
            throw new IllegalStateException("Flight booking failed: " + e.getMessage());
        }
    }

    // ─── Confirm Payment ────────────────────────────────────────────

    @Transactional
    public FlightBookingResponse confirmPayment(UUID userId, UUID bookingId,
                                                 String razorpayOrderId, String razorpayPaymentId) {
        FlightBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Booking does not belong to this user");
        }
        if (booking.getStatus() != FlightBookingStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Booking is not in PENDING_PAYMENT status");
        }

        booking.setRazorpayOrderId(razorpayOrderId);
        booking.setRazorpayPaymentId(razorpayPaymentId);
        booking.setPaymentStatus("PAID");
        booking.setStatus(FlightBookingStatus.CONFIRMED);

        booking = bookingRepository.save(booking);
        log.info("Flight booking confirmed: {} payment: {}", booking.getBookingRef(), razorpayPaymentId);
        publishEvent("flight.booking.confirmed", booking);
        return toResponse(booking);
    }

    // ─── Cancel Booking ─────────────────────────────────────────────

    /**
     * Cancel via Amadeus: DELETE /v1/booking/flight-orders/{orderId}
     */
    @Transactional
    public FlightBookingResponse cancelBooking(UUID userId, UUID bookingId) {
        FlightBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));

        if (!booking.getUserId().equals(userId)) {
            throw new IllegalArgumentException("Booking does not belong to this user");
        }
        if (booking.getStatus() == FlightBookingStatus.CANCELLED
                || booking.getStatus() == FlightBookingStatus.REFUNDED) {
            throw new IllegalStateException("Booking is already cancelled/refunded");
        }

        // Cancel on Amadeus
        if (booking.getDuffelOrderId() != null && !booking.getDuffelOrderId().isBlank()) {
            try {
                String token = getAccessToken();
                amadeusWebClient.delete()
                        .uri("/v1/booking/flight-orders/" + booking.getDuffelOrderId())
                        .header("Authorization", "Bearer " + token)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                log.info("Amadeus order cancelled: {}", booking.getDuffelOrderId());
            } catch (WebClientResponseException e) {
                log.error("Amadeus cancel error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            } catch (Exception e) {
                log.error("Amadeus cancel error: {}", e.getMessage());
            }
        }

        booking.setStatus(FlightBookingStatus.CANCELLED);
        booking.setCancelledAt(Instant.now());
        booking.setCancellationReason("Cancelled by user");
        booking.setRefundAmountPaise(booking.getTotalAmountPaise()); // full refund assumed

        if ("PAID".equals(booking.getPaymentStatus())) {
            booking.setPaymentStatus("REFUND_INITIATED");
            booking.setStatus(FlightBookingStatus.REFUNDED);
        }

        booking = bookingRepository.save(booking);
        log.info("Flight booking cancelled: {}", booking.getBookingRef());
        publishEvent("flight.booking.cancelled", booking);
        return toResponse(booking);
    }

    // ─── Read Operations ────────────────────────────────────────────

    public Page<FlightBookingResponse> getMyBookings(UUID userId, Pageable pageable) {
        return bookingRepository.findByUserId(userId, pageable).map(this::toResponse);
    }

    public FlightBookingResponse getBooking(UUID bookingId) {
        FlightBooking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NoSuchElementException("Booking not found: " + bookingId));
        return toResponse(booking);
    }

    // ─── Private: Build Amadeus Request Bodies ──────────────────────

    private ObjectNode buildSearchBody(FlightSearchRequest request) {
        ObjectNode root = objectMapper.createObjectNode();

        // Origin-Destinations (Amadeus format: flat fields)
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

        // Travelers
        ArrayNode travelers = root.putArray("travelers");
        int pax = request.passengers() != null ? request.passengers() : 1;
        for (int i = 1; i <= pax; i++) {
            ObjectNode traveler = travelers.addObject();
            traveler.put("id", String.valueOf(i));
            traveler.put("travelerType", "ADULT");
        }

        // Search criteria
        ObjectNode searchCriteria = root.putObject("searchCriteria");
        searchCriteria.put("maxFlightOffers", 50);

        if (request.maxConnections() != null) {
            ObjectNode flightFilters = searchCriteria.putObject("flightFilters");
            ObjectNode connectionRestriction = flightFilters.putObject("connectionRestriction");
            connectionRestriction.put("maxNumberOfConnections", request.maxConnections());
        }

        // Cabin
        String cabin = request.cabinClass() != null ? request.cabinClass().toUpperCase() : "ECONOMY";
        ObjectNode cabinRestrictions = searchCriteria.putObject("flightFilters");
        ArrayNode cabinArr = cabinRestrictions.putArray("cabinRestrictions");
        ObjectNode cabinObj = cabinArr.addObject();
        cabinObj.put("cabin", cabin);
        cabinObj.put("coverage", "MOST_SEGMENTS");
        cabinObj.put("originDestinationIds", "1");

        // Sources
        root.putArray("sources").add("GDS");

        return root;
    }

    private ObjectNode buildOrderBody(CreateFlightBookingRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode data = root.putObject("data");
        data.put("type", "flight-order");

        // Flight offers — Amadeus needs the full offer from search
        // In practice, frontend should pass the full offer JSON
        // For now, we reference by ID (works with pricing confirmation)
        ArrayNode flightOffers = data.putArray("flightOffers");
        ObjectNode offer = flightOffers.addObject();
        offer.put("type", "flight-offer");
        offer.put("id", request.offerId());

        // Travelers
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

            // Passport for international
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

        // Contact
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

    // ─── Private: Parse Amadeus Search Response ─────────────────────

    private FlightSearchResponse parseSearchResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode dataArr = root.path("data");
            JsonNode dictionaries = root.path("dictionaries");
            JsonNode carriers = dictionaries.path("carriers");
            JsonNode aircraft = dictionaries.path("aircraft");

            List<FlightSearchResponse.FlightOffer> offers = new ArrayList<>();

            if (dataArr.isArray()) {
                for (JsonNode offerNode : dataArr) {
                    String offerId = offerNode.path("id").asText();

                    // Price
                    JsonNode price = offerNode.path("price");
                    String totalAmount = price.path("grandTotal").asText("0");
                    long pricePaise = Math.round(Double.parseDouble(totalAmount) * 100);
                    String currency = price.path("currency").asText("INR");

                    // First itinerary
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

                    // Cabin class
                    String cabinCls = "Economy";
                    JsonNode travelerPricings = offerNode.path("travelerPricings");
                    if (travelerPricings.isArray() && !travelerPricings.isEmpty()) {
                        cabinCls = travelerPricings.get(0).path("fareDetailsBySegment")
                                .path(0).path("cabin").asText("ECONOMY");
                    }

                    // Segments
                    List<FlightSearchResponse.Segment> segmentList = new ArrayList<>();
                    for (JsonNode seg : segmentsNode) {
                        String segCarrier = seg.path("carrierCode").asText("");
                        String segAirline = carriers.path(segCarrier).asText(segCarrier);
                        String acCode = seg.path("aircraft").path("code").asText("");
                        String acName = aircraft.path(acCode).asText(acCode);

                        segmentList.add(new FlightSearchResponse.Segment(
                                seg.path("id").asText(""),
                                segAirline,
                                segCarrier + seg.path("number").asText(""),
                                seg.path("departure").path("iataCode").asText(""),
                                "", // Amadeus doesn't return city name in search, resolved client-side
                                seg.path("arrival").path("iataCode").asText(""),
                                "",
                                seg.path("departure").path("at").asText(""),
                                seg.path("arrival").path("at").asText(""),
                                seg.path("duration").asText(""),
                                acName
                        ));
                    }

                    // Airline logo — use a standard URL pattern
                    String airlineLogo = "https://pics.avs.io/60/60/" + carrierCode + ".png";

                    offers.add(new FlightSearchResponse.FlightOffer(
                            offerId, airlineName, airlineLogo, flightNum,
                            departureTime, arrivalTime, duration, stops,
                            pricePaise, currency, cabinCls, segmentList
                    ));
                }
            }

            // Sort by price ascending (cheapest first)
            offers.sort(Comparator.comparingLong(FlightSearchResponse.FlightOffer::pricePaise));

            return new FlightSearchResponse(offers);
        } catch (Exception e) {
            log.error("Error parsing Amadeus search response", e);
            throw new IllegalStateException("Failed to parse flight search results");
        }
    }

    // ─── Private: Helpers ───────────────────────────────────────────

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

    private String generateBookingRef() {
        return "SF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private FlightBookingResponse toResponse(FlightBooking b) {
        return new FlightBookingResponse(
                b.getId(), b.getUserId(), b.getBookingRef(), b.getDuffelOrderId(),
                b.getStatus(), b.getTripType(), b.getCabinClass(),
                b.getDepartureCity(), b.getDepartureCityCode(),
                b.getArrivalCity(), b.getArrivalCityCode(),
                b.getDepartureDate(), b.getReturnDate(),
                b.getAirline(), b.getFlightNumber(), b.getIsInternational(),
                b.getPassengersJson(), b.getSlicesJson(),
                b.getTotalAmountPaise(), b.getTaxPaise(), b.getPlatformFeePaise(),
                b.getCurrency(), b.getRazorpayOrderId(), b.getRazorpayPaymentId(),
                b.getPaymentStatus(), b.getContactEmail(), b.getContactPhone(),
                b.getCancellationReason(), b.getCancelledAt(), b.getRefundAmountPaise(),
                b.getCreatedAt(), b.getUpdatedAt()
        );
    }

    private void publishEvent(String topic, FlightBooking booking) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("bookingId", booking.getId().toString());
            event.put("bookingRef", booking.getBookingRef());
            event.put("userId", booking.getUserId().toString());
            event.put("status", booking.getStatus().name());
            event.put("totalAmountPaise", booking.getTotalAmountPaise());
            event.put("taxPaise", booking.getTaxPaise());
            event.put("platformFeePaise", booking.getPlatformFeePaise());
            event.put("currency", Optional.ofNullable(booking.getCurrency()).orElse("INR"));
            event.put("airline", Optional.ofNullable(booking.getAirline()).orElse(""));
            event.put("flightNumber", Optional.ofNullable(booking.getFlightNumber()).orElse(""));
            event.put("departureCity", Optional.ofNullable(booking.getDepartureCity()).orElse(""));
            event.put("departureCityCode", Optional.ofNullable(booking.getDepartureCityCode()).orElse(""));
            event.put("arrivalCity", Optional.ofNullable(booking.getArrivalCity()).orElse(""));
            event.put("arrivalCityCode", Optional.ofNullable(booking.getArrivalCityCode()).orElse(""));
            event.put("departureDate", booking.getDepartureDate().toString());
            event.put("returnDate", booking.getReturnDate() != null ? booking.getReturnDate().toString() : "");
            event.put("tripType", booking.getTripType().name());
            event.put("isInternational", booking.getIsInternational());
            event.put("contactEmail", Optional.ofNullable(booking.getContactEmail()).orElse(""));
            event.put("contactPhone", Optional.ofNullable(booking.getContactPhone()).orElse(""));
            if (booking.getRefundAmountPaise() != null) event.put("refundAmountPaise", booking.getRefundAmountPaise());
            if (booking.getPaymentStatus() != null) event.put("paymentStatus", booking.getPaymentStatus());

            kafkaTemplate.send(topic, booking.getId().toString(), objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Failed to publish Kafka event: {}", topic, e);
        }
    }
}
