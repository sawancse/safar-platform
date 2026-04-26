package com.safar.flight.adapter.duffel;

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
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.LocalDate;
import java.util.*;

/**
 * Duffel API v2 — full bookable adapter (search, book, cancel + refund).
 * Self-serve sign-up with sandbox; same endpoints for live + test mode,
 * mode is determined by the access token type.
 *
 * Docs: https://duffel.com/docs/api/overview/welcome
 * Auth: Bearer token in Authorization header
 * Required headers: Duffel-Version: v2
 * Amounts: Duffel uses decimal strings in major units ("234.50") — we
 * convert to/from paise at the boundary.
 */
@Component
@Slf4j
public class DuffelFlightAdapter implements FlightProviderAdapter {

    private static final String API_VERSION_HEADER = "Duffel-Version";

    private final WebClient duffelWebClient;
    private final ObjectMapper objectMapper;

    @Value("${duffel.access-token:}")
    private String accessToken;

    @Value("${duffel.api-version:v2}")
    private String apiVersion;

    @Value("${duffel.payment-currency:INR}")
    private String paymentCurrency;

    @Value("${duffel.enabled:true}")
    private boolean enabled;

    public DuffelFlightAdapter(@Qualifier("duffelWebClient") WebClient duffelWebClient,
                               ObjectMapper objectMapper) {
        this.duffelWebClient = duffelWebClient;
        this.objectMapper = objectMapper;
    }

    @Override public FlightProvider providerType() { return FlightProvider.DUFFEL; }

    @Override
    public boolean isEnabled() {
        return enabled && accessToken != null && !accessToken.isBlank();
    }

    @Override public boolean canBook() { return true; }

    // ─── Search ─────────────────────────────────────────────────────

    @Override
    public List<FlightOffer> search(FlightSearchRequest request) {
        try {
            ObjectNode body = buildOfferRequestBody(request);

            String responseJson = duffelWebClient.post()
                    .uri(uri -> uri.path("/air/offer_requests").queryParam("return_offers", true).build())
                    .header("Authorization", "Bearer " + accessToken)
                    .header(API_VERSION_HEADER, apiVersion)
                    .bodyValue(body.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseOfferRequest(responseJson);
        } catch (WebClientResponseException e) {
            log.error("Duffel search error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FlightProviderException("Duffel search failed: " + e.getStatusCode(), e);
        } catch (FlightProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Duffel search error", e);
            throw new FlightProviderException("Duffel search failed: " + e.getMessage(), e);
        }
    }

    // ─── Book ───────────────────────────────────────────────────────

    @Override
    public ProviderBookingResult book(String nativeOfferId,
                                      CreateFlightBookingRequest request,
                                      UUID userId) {
        try {
            // Step 1: re-fetch the offer to get the current price and the
            // server-assigned passenger IDs Duffel requires on the order.
            String offerJson = duffelWebClient.get()
                    .uri("/air/offers/" + nativeOfferId)
                    .header("Authorization", "Bearer " + accessToken)
                    .header(API_VERSION_HEADER, apiVersion)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode offerData = objectMapper.readTree(offerJson).path("data");

            String totalAmount = offerData.path("total_amount").asText("0");
            String currency = offerData.path("total_currency").asText(paymentCurrency);
            long totalPaise = decimalToPaise(totalAmount);
            long taxPaise = decimalToPaise(offerData.path("tax_amount").asText("0"));

            // Step 2: build + place the order
            ObjectNode orderBody = buildOrderBody(nativeOfferId, request, offerData, totalAmount, currency);

            String responseJson = duffelWebClient.post()
                    .uri("/air/orders")
                    .header("Authorization", "Bearer " + accessToken)
                    .header(API_VERSION_HEADER, apiVersion)
                    .bodyValue(orderBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode orderData = objectMapper.readTree(responseJson).path("data");
            String orderId = orderData.path("id").asText("");
            String bookingReference = orderData.path("booking_reference").asText("");

            // Reuse offer slices for metadata since order response shape is similar
            JsonNode slices = orderData.has("slices") && orderData.path("slices").isArray()
                    ? orderData.path("slices") : offerData.path("slices");

            JsonNode firstSlice = slices.isArray() && !slices.isEmpty()
                    ? slices.get(0) : objectMapper.createObjectNode();
            JsonNode segments = firstSlice.path("segments");
            JsonNode firstSegment = segments.isArray() && !segments.isEmpty()
                    ? segments.get(0) : objectMapper.createObjectNode();
            JsonNode lastSegment = segments.isArray() && !segments.isEmpty()
                    ? segments.get(segments.size() - 1) : firstSegment;

            String departureCityCode = firstSegment.path("origin").path("iata_code").asText("");
            String arrivalCityCode = lastSegment.path("destination").path("iata_code").asText("");
            String airline = firstSegment.path("marketing_carrier").path("iata_code").asText("");
            String flightNumber = airline + firstSegment.path("marketing_carrier_flight_number").asText("");

            boolean isInternational = !isIndianAirport(departureCityCode) || !isIndianAirport(arrivalCityCode);

            int sliceCount = slices.isArray() ? slices.size() : 1;
            TripType tripType = sliceCount > 1 ? TripType.ROUND_TRIP : TripType.ONE_WAY;

            LocalDate departureDate = parseDateOrNull(firstSegment.path("departing_at").asText(""));
            LocalDate returnDate = null;
            if (tripType == TripType.ROUND_TRIP && slices.size() > 1) {
                JsonNode returnSlice = slices.get(1);
                JsonNode rs = returnSlice.path("segments");
                if (rs.isArray() && !rs.isEmpty()) {
                    returnDate = parseDateOrNull(rs.get(0).path("departing_at").asText(""));
                }
            }

            String cabinStr = firstSegment.path("passengers").path(0).path("cabin_class").asText("economy");
            CabinClass cabinClass = mapCabinClass(cabinStr);

            log.info("Duffel order created: {} (ref={})", orderId, bookingReference);
            return new ProviderBookingResult(
                    orderId, "CONFIRMED", totalPaise, taxPaise, currency,
                    departureCityCode, arrivalCityCode,
                    departureDate != null ? departureDate : LocalDate.now().plusDays(1),
                    returnDate, tripType, cabinClass, airline, flightNumber, isInternational,
                    slices.toString()
            );

        } catch (WebClientResponseException e) {
            log.error("Duffel book error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FlightProviderException("Duffel booking failed: " + e.getStatusCode(), e);
        } catch (FlightProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Duffel book error", e);
            throw new FlightProviderException("Duffel booking failed: " + e.getMessage(), e);
        }
    }

    // ─── Cancel + Refund ────────────────────────────────────────────

    /**
     * Two-step cancel: create an Order Cancellation (returns refund amount
     * + cancellation id), then confirm it. Confirm initiates the refund
     * with the airline. Refund amount is determined by Duffel against the
     * fare rules.
     */
    @Override
    public void cancel(String externalOrderId) {
        if (externalOrderId == null || externalOrderId.isBlank()) {
            return;
        }
        try {
            ObjectNode createBody = objectMapper.createObjectNode();
            createBody.putObject("data").put("order_id", externalOrderId);

            String createResp = duffelWebClient.post()
                    .uri("/air/order_cancellations")
                    .header("Authorization", "Bearer " + accessToken)
                    .header(API_VERSION_HEADER, apiVersion)
                    .bodyValue(createBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode cancellation = objectMapper.readTree(createResp).path("data");
            String cancellationId = cancellation.path("id").asText("");
            String refundAmount = cancellation.path("refund_amount").asText("0");
            String refundCurrency = cancellation.path("refund_currency").asText(paymentCurrency);
            log.info("Duffel cancellation proposed for {}: refund {} {}",
                    externalOrderId, refundAmount, refundCurrency);

            duffelWebClient.post()
                    .uri("/air/order_cancellations/" + cancellationId + "/actions/confirm")
                    .header("Authorization", "Bearer " + accessToken)
                    .header(API_VERSION_HEADER, apiVersion)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            log.info("Duffel cancellation confirmed: {} (refund {} {})",
                    cancellationId, refundAmount, refundCurrency);

        } catch (WebClientResponseException e) {
            log.error("Duffel cancel error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new FlightProviderException("Duffel cancel failed: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Duffel cancel error: {}", e.getMessage());
            throw new FlightProviderException("Duffel cancel failed: " + e.getMessage(), e);
        }
    }

    // ─── Build request bodies ───────────────────────────────────────

    private ObjectNode buildOfferRequestBody(FlightSearchRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode data = root.putObject("data");

        ArrayNode slices = data.putArray("slices");
        ObjectNode outbound = slices.addObject();
        outbound.put("origin", request.origin());
        outbound.put("destination", request.destination());
        outbound.put("departure_date", request.departureDate().toString());

        if (request.returnDate() != null) {
            ObjectNode inbound = slices.addObject();
            inbound.put("origin", request.destination());
            inbound.put("destination", request.origin());
            inbound.put("departure_date", request.returnDate().toString());
        }

        ArrayNode passengers = data.putArray("passengers");
        int pax = request.passengers() != null ? request.passengers() : 1;
        for (int i = 0; i < pax; i++) {
            passengers.addObject().put("type", "adult");
        }

        data.put("cabin_class", request.cabinClass() != null
                ? request.cabinClass().toLowerCase() : "economy");
        return root;
    }

    private ObjectNode buildOrderBody(String offerId,
                                      CreateFlightBookingRequest request,
                                      JsonNode offerData,
                                      String totalAmount,
                                      String currency) {
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode data = root.putObject("data");

        data.put("type", "instant");
        ArrayNode selected = data.putArray("selected_offers");
        selected.add(offerId);

        // Map our passengers to Duffel-assigned passenger IDs (positional).
        JsonNode duffelPassengers = offerData.path("passengers");
        ArrayNode passengers = data.putArray("passengers");
        for (int i = 0; i < request.passengers().size(); i++) {
            PassengerDTO p = request.passengers().get(i);
            String duffelPaxId = i < duffelPassengers.size()
                    ? duffelPassengers.get(i).path("id").asText("") : "";

            ObjectNode pax = passengers.addObject();
            pax.put("id", duffelPaxId);
            pax.put("title", "mr");                 // Duffel requires; default mr
            pax.put("given_name", p.firstName());
            pax.put("family_name", p.lastName());
            pax.put("born_on", p.dateOfBirth().toString());
            pax.put("gender", p.gender() != null
                    ? p.gender().toLowerCase().substring(0, 1) : "m");      // m / f
            pax.put("email", request.contactEmail());
            pax.put("phone_number", normalizePhone(request.contactPhone()));
        }

        // Payment: settle from our Duffel balance; we collect from user via Razorpay.
        ArrayNode payments = data.putArray("payments");
        ObjectNode payment = payments.addObject();
        payment.put("type", "balance");
        payment.put("amount", totalAmount);
        payment.put("currency", currency);

        return root;
    }

    // ─── Parse search response ──────────────────────────────────────

    private List<FlightOffer> parseOfferRequest(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode offers = root.path("data").path("offers");

            List<FlightOffer> result = new ArrayList<>();
            if (!offers.isArray()) return result;

            for (JsonNode offer : offers) {
                String nativeId = offer.path("id").asText();
                long pricePaise = decimalToPaise(offer.path("total_amount").asText("0"));
                String currency = offer.path("total_currency").asText(paymentCurrency);

                JsonNode slices = offer.path("slices");
                if (!slices.isArray() || slices.isEmpty()) continue;
                JsonNode firstSlice = slices.get(0);
                JsonNode segmentsNode = firstSlice.path("segments");
                if (!segmentsNode.isArray() || segmentsNode.isEmpty()) continue;

                JsonNode firstSeg = segmentsNode.get(0);
                JsonNode lastSeg = segmentsNode.get(segmentsNode.size() - 1);

                String carrierCode = firstSeg.path("marketing_carrier").path("iata_code").asText("");
                String airlineName = firstSeg.path("marketing_carrier").path("name").asText(carrierCode);
                String flightNum = carrierCode + firstSeg.path("marketing_carrier_flight_number").asText("");
                String departureTime = firstSeg.path("departing_at").asText("");
                String arrivalTime = lastSeg.path("arriving_at").asText("");
                String duration = firstSlice.path("duration").asText("");
                int stops = segmentsNode.size() - 1;

                String cabinCls = firstSeg.path("passengers").path(0).path("cabin_class").asText("economy").toUpperCase();

                List<Segment> segmentList = new ArrayList<>();
                for (JsonNode seg : segmentsNode) {
                    String segCarrier = seg.path("marketing_carrier").path("iata_code").asText("");
                    String segAirline = seg.path("marketing_carrier").path("name").asText(segCarrier);
                    String aircraft = seg.path("aircraft").path("name").asText("");

                    segmentList.add(new Segment(
                            seg.path("id").asText(""),
                            segAirline,
                            segCarrier + seg.path("marketing_carrier_flight_number").asText(""),
                            seg.path("origin").path("iata_code").asText(""),
                            seg.path("origin").path("city_name").asText(""),
                            seg.path("destination").path("iata_code").asText(""),
                            seg.path("destination").path("city_name").asText(""),
                            seg.path("departing_at").asText(""),
                            seg.path("arriving_at").asText(""),
                            seg.path("duration").asText(""),
                            aircraft
                    ));
                }

                String airlineLogo = firstSeg.path("marketing_carrier").path("logo_symbol_url").asText(null);
                if (airlineLogo == null || airlineLogo.isBlank()) {
                    airlineLogo = "https://pics.avs.io/60/60/" + carrierCode + ".png";
                }

                result.add(new FlightOffer(
                        ProviderOfferId.encode(FlightProvider.DUFFEL, nativeId),
                        airlineName, airlineLogo, flightNum,
                        departureTime, arrivalTime, duration, stops,
                        pricePaise, currency, cabinCls, segmentList,
                        FlightProvider.DUFFEL
                ));
            }
            return result;
        } catch (Exception e) {
            log.error("Error parsing Duffel offer_requests response", e);
            throw new FlightProviderException("Failed to parse Duffel search results", e);
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────

    private static long decimalToPaise(String decimal) {
        if (decimal == null || decimal.isBlank()) return 0;
        try {
            return Math.round(Double.parseDouble(decimal) * 100);
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }

    private static LocalDate parseDateOrNull(String iso) {
        if (iso == null || iso.length() < 10) return null;
        try { return LocalDate.parse(iso.substring(0, 10)); }
        catch (Exception e) { return null; }
    }

    private static CabinClass mapCabinClass(String duffelCabin) {
        if (duffelCabin == null) return CabinClass.ECONOMY;
        return switch (duffelCabin.toLowerCase()) {
            case "premium_economy" -> CabinClass.PREMIUM_ECONOMY;
            case "business" -> CabinClass.BUSINESS;
            case "first" -> CabinClass.FIRST;
            default -> CabinClass.ECONOMY;
        };
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return "";
        String digits = phone.replaceAll("[^0-9+]", "");
        return digits.startsWith("+") ? digits : ("+91" + digits);
    }

    private static final Set<String> INDIAN_AIRPORTS = Set.of(
            "DEL", "BOM", "BLR", "HYD", "MAA", "CCU", "GOI", "COK", "JAI", "AMD",
            "PNQ", "LKO", "GAU", "SXR", "IXC", "PAT", "BBI", "NAG", "VNS", "IXB",
            "TRV", "VTZ", "IDR", "STV", "UDR", "RPR", "IXR", "CJB", "IXM", "TRZ",
            "IXA", "IMF", "DIB", "DED", "IXJ", "VGA", "HBX", "IXL", "AGR", "BHO",
            "DBR", "GYA", "BDQ", "JDH", "CNN", "MYQ", "IXE", "DHM", "SAG", "IXD",
            "GOP", "KNU", "TIR", "IXZ", "AYJ"
    );

    private static boolean isIndianAirport(String iataCode) {
        return INDIAN_AIRPORTS.contains(iataCode);
    }
}
