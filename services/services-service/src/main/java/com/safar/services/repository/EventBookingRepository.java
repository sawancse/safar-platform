package com.safar.services.repository;

import com.safar.services.entity.EventBooking;
import com.safar.services.entity.enums.EventBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface EventBookingRepository extends JpaRepository<EventBooking, UUID> {

    List<EventBooking> findByCustomerId(UUID customerId);

    List<EventBooking> findByChefId(UUID chefId);

    List<EventBooking> findByStatus(EventBookingStatus status);

    List<EventBooking> findByChefIdAndEventDate(UUID chefId, LocalDate eventDate);

    /**
     * V28 — bookings assigned to the given vendor's PartnerVendor row(s).
     * Resolves vendor via partner_vendors.user_id (set on auto-stub at
     * ServiceListing approve time). Returns newest first.
     */
    @Query(value = """
        SELECT eb.* FROM chefs.event_bookings eb
        JOIN chefs.event_booking_vendor v ON v.event_booking_id = eb.id
        JOIN chefs.partner_vendors pv     ON pv.id = v.vendor_id
        WHERE pv.user_id = :userId
        ORDER BY eb.created_at DESC
        """, nativeQuery = true)
    List<EventBooking> findAssignedToVendorUser(@Param("userId") UUID userId);

    /**
     * V28 — open inquiries the given vendor could claim. Filters:
     *   - no chef assigned (chef_id NULL)
     *   - no live (non-CANCELLED) vendor row attached
     *   - menu_description JSON's `type` field matches one of the supplied
     *     vendor service-types (e.g. PANDIT_PUJA, DESIGNER_CAKE)
     *   - city matches (vendor's serving cities) OR vendor accepts everywhere
     *
     * Cities arrive lowercased. An empty cities list disables the city filter
     * (vendor serves everywhere).
     */
    @Query(value = """
        SELECT eb.* FROM chefs.event_bookings eb
        WHERE eb.chef_id IS NULL
          AND NOT EXISTS (
                SELECT 1 FROM chefs.event_booking_vendor v
                WHERE v.event_booking_id = eb.id
                  AND v.status <> 'CANCELLED'
          )
          AND eb.menu_description IS NOT NULL
          AND (eb.menu_description::jsonb ->> 'type') = ANY(CAST(:types AS text[]))
          AND (
                cardinality(CAST(:cities AS text[])) = 0
             OR LOWER(eb.city) = ANY(CAST(:cities AS text[]))
          )
        ORDER BY eb.event_date ASC NULLS LAST, eb.created_at DESC
        """, nativeQuery = true)
    List<EventBooking> findOpenInquiries(@Param("types") String[] types,
                                          @Param("cities") String[] cities);

    /**
     * Backfill query — bookings past the CONFIRMED gate that are missing the
     * start-job OTP. Used at startup to repair legacy rows so the customer's
     * OTP tab always has something to share with the partner.
     */
    @Query(value = """
        SELECT * FROM chefs.event_bookings
        WHERE start_job_otp IS NULL
          AND status IN ('CONFIRMED', 'ADVANCE_PAID', 'IN_PROGRESS')
        """, nativeQuery = true)
    List<EventBooking> findMissingStartJobOtp();
}
