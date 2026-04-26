package com.safar.flight.adapter.tbo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.flight.adapter.*;
import com.safar.flight.dto.CreateFlightBookingRequest;
import com.safar.flight.dto.FlightSearchRequest;
import com.safar.flight.dto.FlightSearchResponse.FlightOffer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * TBO Air integration — STUB / SCAFFOLD.
 *
 * Architecture-locked, HTTP-not-wired-yet. Becomes live once partner
 * onboarding completes and sandbox credentials are issued.
 *
 * Integration plan reference:
 *   _bmad/docs/brainstorming/brainstorming-session-2026-04-26-tbo-design-plan.md
 *
 * TBO API contract (public docs at dealint.tboair.com — login required):
 *   1. POST /Authenticate          → returns TokenId (cache for ~24h)
 *   2. POST /Search                → returns ResultIndex offers
 *   3. POST /FareQuote             → re-confirms price + returns IsPriceChanged flag
 *   4. POST /Book                  → creates booking, returns BookingId + PNR
 *   5. POST /SendCancellationRequest  → initiates cancel + refund
 *
 * Auth: cached session token — re-authenticate on 401 with auto-retry.
 * Currency: INR (TBO is Indian agent of record).
 *
 * To make live:
 *   1. TBO partnership signed → sandbox creds issued
 *   2. Set env: TBO_USERNAME, TBO_PASSWORD, TBO_AGENCY_ID
 *   3. Replace 5 TODO(tbo-creds) blocks with real RestClient calls
 *   4. Sandbox-test 50+ POs end-to-end before promoting to prod
 *   5. Set FLIGHT_PRIMARY_PROVIDER=TBO for India routes (per Phase-2 Tree-2 routing)
 */
@Component
@Slf4j
public class TBOAirFlightAdapter implements FlightProviderAdapter {

    private final WebClient tboWebClient;
    private final ObjectMapper objectMapper;

    @Value("${tbo.enabled:false}")
    private boolean enabled;

    @Value("${tbo.username:}")
    private String username;

    @Value("${tbo.password:}")
    private String password;

    @Value("${tbo.agency-id:}")
    private String agencyId;

    @Value("${tbo.payment-currency:INR}")
    private String paymentCurrency;

    private final AtomicReference<String> sessionToken = new AtomicReference<>();
    private volatile long tokenExpiresAt = 0;

    public TBOAirFlightAdapter(@Qualifier("tboWebClient") WebClient tboWebClient,
                                ObjectMapper objectMapper) {
        this.tboWebClient = tboWebClient;
        this.objectMapper = objectMapper;
    }

    @Override public FlightProvider providerType() { return FlightProvider.TBO; }

    @Override
    public boolean isEnabled() {
        return enabled
                && username != null && !username.isBlank()
                && password != null && !password.isBlank()
                && agencyId != null && !agencyId.isBlank();
    }

    @Override public boolean canBook() { return true; }

    // ─── Auth: cached session token ─────────────────────────────────

    /**
     * TBO returns a TokenId valid for ~24h on Authenticate. We cache and
     * re-issue on 401 from any subsequent call.
     */
    private synchronized String getSessionToken() {
        if (sessionToken.get() != null && System.currentTimeMillis() < tokenExpiresAt - 60_000) {
            return sessionToken.get();
        }
        return refreshSessionToken();
    }

    private String refreshSessionToken() {
        // TODO(tbo-creds): replace with real call once sandbox is open.
        // Endpoint: POST /Authenticate
        // Body: {
        //   "ClientId": "{TBO_CLIENT_ID}",
        //   "UserName": "{username}",
        //   "Password": "{password}",
        //   "EndUserIp": "{server-ip}"
        // }
        // Response: { "TokenId": "...", "Member": { "AgencyId": ..., ... } }
        // Set tokenExpiresAt = now + 23h (TBO docs say 24h; leave 1h buffer)
        log.warn("TBOAirFlightAdapter.refreshSessionToken not implemented — sandbox creds pending");
        throw new FlightProviderException("TBOAirFlightAdapter not yet live (sandbox creds pending)");
    }

    // ─── Search ─────────────────────────────────────────────────────

    @Override
    public List<FlightOffer> search(FlightSearchRequest request) {
        // TODO(tbo-creds): wire real call once sandbox is open.
        // Endpoint: POST /Search
        // Body shape:
        //   {
        //     "EndUserIp": "...",
        //     "TokenId":   "{session token from Authenticate}",
        //     "AdultCount": pax,
        //     "ChildCount": 0,
        //     "InfantCount": 0,
        //     "DirectFlight": false,
        //     "OneStopFlight": false,
        //     "JourneyType": 1,           // 1=OneWay, 2=Return, 3=MultiCity
        //     "Segments": [{
        //       "Origin": "DEL",
        //       "Destination": "BOM",
        //       "FlightCabinClass": 1-6,    // 1=All, 2=Economy, 3=PremiumEconomy, 4=Business, 5=PremiumBusiness, 6=First
        //       "PreferredDepartureTime": "2026-05-15T00:00:00",
        //       "PreferredArrivalTime":   "2026-05-15T00:00:00"
        //     }],
        //     "Sources": ["GDS","LCC"]   // optional supplier filter
        //   }
        //
        // Response: data.Results[][] of offers, each containing:
        //   ResultIndex, Source, IsLCC, AirlineCode, Fare {PublishedFare,Tax,...},
        //   Segments[][] (outbound + return slice arrays)
        //
        // Per offer → build FlightOffer using:
        //   ProviderOfferId.encode(FlightProvider.TBO, ResultIndex)
        //   - airline + flight number from Segments[0][0]
        //   - duration computed from Departure→Arrival
        //   - stops = Segments[0].length - 1
        //   - pricePaise = round(Fare.PublishedFare * 100)
        //   - currency = INR
        //   - cabinClass mapped from FlightCabinClass
        //   - bookingMode = BOOKABLE
        //
        // Return List<FlightOffer> sorted by price ASC.
        log.warn("TBOAirFlightAdapter.search not implemented — sandbox creds pending");
        throw new FlightProviderException("TBOAirFlightAdapter not yet live (sandbox creds pending)");
    }

    // ─── Book (FareQuote → Book, two-step) ──────────────────────────

    @Override
    public ProviderBookingResult book(String nativeOfferId,
                                      CreateFlightBookingRequest request,
                                      UUID userId) {
        // TODO(tbo-creds): wire real call once sandbox is open.
        // STEP 1 — POST /FareQuote (re-confirm price; TBO fares may shift between search and book)
        //   Body: { "EndUserIp": "...", "TokenId": "...", "ResultIndex": "{nativeOfferId}", "TraceId": "..." }
        //   Response: { "Results": { "Fare": {...}, "IsPriceChanged": false, "FareRules": [...] } }
        //   If IsPriceChanged == true → bail out, surface to user (per Tree-2 hybrid retry policy).
        //
        // STEP 2 — POST /Book (create PNR)
        //   Body: {
        //     "EndUserIp": "...", "TokenId": "...",
        //     "ResultIndex": "{nativeOfferId}", "TraceId": "...",
        //     "Passengers": [{
        //       "Title": "Mr"|"Mrs"|"Miss"|"Master",
        //       "FirstName": p.firstName, "LastName": p.lastName,
        //       "PaxType": 1,                  // 1=Adult, 2=Child, 3=Infant
        //       "DateOfBirth": p.dateOfBirth,
        //       "Gender": 1|2,                 // 1=Male, 2=Female
        //       "AddressLine1": "...", "City": "...", "CountryCode": "IN",
        //       "Nationality": "IN",
        //       "ContactNo": request.contactPhone,
        //       "Email": request.contactEmail,
        //       "IsLeadPax": (i==0),
        //       "Fare": { ... },
        //       "PassportNo": p.passportNumber,    // intl only
        //       "PassportExpiry": p.passportExpiry
        //     }]
        //   }
        //   Response: { "Response": { "BookingId": ..., "PNR": "...", "Status": ... } }
        //
        // Map response to ProviderBookingResult (same shape as Duffel adapter does):
        //   - externalOrderId  = BookingId  (or PNR if BookingId not present)
        //   - providerStatus   = "CONFIRMED" if Status == 1, else "PENDING"
        //   - totalAmountPaise = round(Fare.PublishedFare * 100)
        //   - taxPaise         = round(Fare.Tax * 100)
        //   - currency         = "INR"
        //   - departureCityCode/arrivalCityCode/dates/airline/flightNumber from Segments[0][0]
        //   - tripType         = JourneyType-derived (1=OneWay, 2=RoundTrip)
        //   - cabinClass       = mapTboCabin(FlightCabinClass)
        //   - isInternational  = !isIndianAirport(dep) || !isIndianAirport(arr)
        //   - itinerariesJson  = serialize Segments[][]
        log.warn("TBOAirFlightAdapter.book not implemented — sandbox creds pending");
        throw new FlightProviderException("TBOAirFlightAdapter not yet live (sandbox creds pending)");
    }

    // ─── Cancel + Refund ────────────────────────────────────────────

    @Override
    public void cancel(String externalOrderId) {
        if (externalOrderId == null || externalOrderId.isBlank()) return;

        // TODO(tbo-creds): wire real call once sandbox is open.
        // Endpoint: POST /SendCancellationRequest
        // Body: {
        //   "EndUserIp": "...", "TokenId": "...",
        //   "BookingId": {externalOrderId}, "RequestType": 4  // 1=Refund, 2=Cancellation, 4=FullRefund-no-show-allowed
        // }
        // Response: { "ChangeRequestId": "...", "Status": ... }
        //
        // TBO refund flow:
        //   1. /SendCancellationRequest → returns ChangeRequestId
        //   2. (Async) TBO airline coordinator processes the request
        //   3. Webhook callback (or status-poll job) → confirms refund amount
        //   4. We update FlightBooking.refundAmountPaise + status = REFUNDED
        //
        // NOTE: TBO is async by design — refund amount is NOT returned synchronously.
        // We mark the booking CANCELLED locally; refund status updates via separate flow.
        log.warn("TBOAirFlightAdapter.cancel not implemented — sandbox creds pending");
        throw new FlightProviderException("TBOAirFlightAdapter not yet live (sandbox creds pending)");
    }
}
