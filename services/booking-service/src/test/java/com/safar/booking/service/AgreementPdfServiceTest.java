package com.safar.booking.service;

import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyAgreement;
import com.safar.booking.entity.enums.AgreementStatus;
import com.safar.booking.entity.enums.TenancyStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgreementPdfServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private PgTenancyService tenancyService;

    @InjectMocks
    private AgreementPdfService pdfService;

    // ── PG Agreement ──────────────────────────────────────────

    @Test
    @DisplayName("PG agreement HTML contains all key fields")
    void pgAgreementHtml_containsAllFields() {
        PgTenancy tenancy = buildPgTenancy();
        TenancyAgreement agreement = buildPgAgreement();

        // Capture the context passed to Thymeleaf
        when(templateEngine.process(eq("agreement-pdf"), any(Context.class)))
                .thenAnswer(inv -> {
                    Context ctx = inv.getArgument(1);

                    // Verify all key variables are set
                    assertEquals("AGR-2026-0001", ctx.getVariable("agreementNumber"));
                    assertEquals("Rahul Sharma", ctx.getVariable("tenantName"));
                    assertEquals("Suresh Kumar", ctx.getVariable("hostName"));
                    assertEquals("DOUBLE", ctx.getVariable("sharingType"));

                    // Financial
                    assertEquals("\u20B912,000", ctx.getVariable("rentFormatted"));
                    assertEquals("\u20B92,000", ctx.getVariable("maintenanceFormatted"));
                    assertEquals("\u20B914,000", ctx.getVariable("totalFormatted"));
                    assertEquals("\u20B924,000", ctx.getVariable("depositFormatted"));

                    // Penalty config
                    assertEquals(5, ctx.getVariable("gracePeriodDays"));
                    assertEquals("2.0%", ctx.getVariable("penaltyPercent"));
                    assertEquals("25%", ctx.getVariable("maxPenaltyPercent"));
                    assertEquals("1st", ctx.getVariable("billingDayOrdinal"));

                    // Inclusions
                    assertEquals(true, ctx.getVariable("mealsIncluded"));
                    assertEquals(true, ctx.getVariable("wifiIncluded"));
                    assertEquals(false, ctx.getVariable("laundryIncluded"));

                    // Lock-in & notice
                    assertEquals(6, ctx.getVariable("lockInMonths"));
                    assertEquals(30, ctx.getVariable("noticeDays"));

                    return "<html>PG Agreement</html>";
                });

        String html = pdfService.renderHtml(agreement, tenancy);
        assertNotNull(html);
    }

    // ── Apartment Agreement ───────────────────────────────────

    @Test
    @DisplayName("Apartment agreement HTML has different penalty config")
    void apartmentAgreementHtml_hasDifferentConfig() {
        PgTenancy tenancy = buildApartmentTenancy();
        TenancyAgreement agreement = buildApartmentAgreement();

        when(templateEngine.process(eq("agreement-pdf"), any(Context.class)))
                .thenAnswer(inv -> {
                    Context ctx = inv.getArgument(1);

                    assertEquals("Priya Patel", ctx.getVariable("tenantName"));
                    assertEquals("PRIVATE", ctx.getVariable("sharingType"));

                    // Apartment financial
                    assertEquals("\u20B935,000", ctx.getVariable("rentFormatted"));
                    assertEquals("\u20B93,500", ctx.getVariable("maintenanceFormatted"));
                    // Java's %,d uses system locale grouping — verify amount is correct
                    String deposit = (String) ctx.getVariable("depositFormatted");
                    assertTrue(deposit.contains("100,000") || deposit.contains("1,00,000"),
                            "Deposit should be ₹100,000 or ₹1,00,000, got: " + deposit);

                    // Apartment penalty config (different from PG)
                    assertEquals(5, ctx.getVariable("gracePeriodDays"));
                    assertEquals("1.0%", ctx.getVariable("penaltyPercent"));
                    assertEquals("20%", ctx.getVariable("maxPenaltyPercent"));
                    assertEquals("15th", ctx.getVariable("billingDayOrdinal"));

                    // No meals/laundry/wifi for apartment
                    assertEquals(false, ctx.getVariable("mealsIncluded"));
                    assertEquals(false, ctx.getVariable("laundryIncluded"));
                    assertEquals(false, ctx.getVariable("wifiIncluded"));

                    // 11-month lock-in, 60-day notice
                    assertEquals(11, ctx.getVariable("lockInMonths"));
                    assertEquals(60, ctx.getVariable("noticeDays"));

                    return "<html>Apartment Agreement</html>";
                });

        String html = pdfService.renderHtml(agreement, tenancy);
        assertNotNull(html);
    }

    // ── Signed Agreement ──────────────────────────────────────

    @Test
    @DisplayName("Signed agreement has signature timestamps")
    void signedAgreement_hasSignatureTimestamps() {
        PgTenancy tenancy = buildPgTenancy();
        TenancyAgreement agreement = buildPgAgreement();
        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setHostSignedAt(OffsetDateTime.now().minusHours(2));
        agreement.setHostSignatureIp("192.168.1.10");
        agreement.setTenantSignedAt(OffsetDateTime.now());
        agreement.setTenantSignatureIp("10.0.0.5");

        when(templateEngine.process(eq("agreement-pdf"), any(Context.class)))
                .thenAnswer(inv -> {
                    Context ctx = inv.getArgument(1);

                    assertNotNull(ctx.getVariable("hostSignedAt"));
                    assertEquals("192.168.1.10", ctx.getVariable("hostSignatureIp"));
                    assertNotNull(ctx.getVariable("tenantSignedAt"));
                    assertEquals("10.0.0.5", ctx.getVariable("tenantSignatureIp"));

                    return "<html>Signed Agreement</html>";
                });

        String html = pdfService.renderHtml(agreement, tenancy);
        assertNotNull(html);
    }

    // ── PDF Generation ────────────────────────────────────────

    @Test
    @DisplayName("PDF generation produces valid bytes")
    void pdfGeneration_producesValidBytes() {
        PgTenancy tenancy = buildPgTenancy();
        TenancyAgreement agreement = buildPgAgreement();

        doReturn(tenancy).when(tenancyService).getTenancy(agreement.getTenancyId());

        // Return minimal valid HTML for PDF rendering
        when(templateEngine.process(eq("agreement-pdf"), any(Context.class)))
                .thenReturn("<html><body><h1>Test Agreement</h1><p>Content</p></body></html>");

        byte[] pdf = pdfService.generatePdf(agreement);

        assertNotNull(pdf);
        assertTrue(pdf.length > 100, "PDF should have content");
        // PDF magic bytes: %PDF
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    // ── Helpers ───────────────────────────────────────────────

    private PgTenancy buildPgTenancy() {
        return PgTenancy.builder()
                .id(UUID.randomUUID())
                .tenancyRef("PGT-2026-0001")
                .tenantId(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .sharingType("DOUBLE")
                .moveInDate(LocalDate.of(2026, 4, 1))
                .noticePeriodDays(30)
                .monthlyRentPaise(1200000)
                .securityDepositPaise(2400000)
                .totalMonthlyPaise(1400000)
                .mealsIncluded(true)
                .laundryIncluded(false)
                .wifiIncluded(true)
                .billingDay(1)
                .gracePeriodDays(5)
                .latePenaltyBps(200)
                .maxPenaltyPercent(25)
                .status(TenancyStatus.ACTIVE)
                .build();
    }

    private TenancyAgreement buildPgAgreement() {
        return TenancyAgreement.builder()
                .id(UUID.randomUUID())
                .tenancyId(UUID.randomUUID())
                .agreementNumber("AGR-2026-0001")
                .tenantName("Rahul Sharma")
                .tenantPhone("+919876543210")
                .tenantEmail("rahul@example.com")
                .tenantAadhaarLast4("7890")
                .hostName("Suresh Kumar")
                .hostPhone("+919999900001")
                .propertyAddress("42, 4th Cross, 6th Block, Koramangala, Bangalore - 560095")
                .roomDescription("Room 3, Bed B (Double Sharing)")
                .moveInDate(LocalDate.of(2026, 4, 1))
                .lockInPeriodMonths(6)
                .noticePeriodDays(30)
                .monthlyRentPaise(1200000)
                .securityDepositPaise(2400000)
                .maintenanceChargesPaise(200000)
                .status(AgreementStatus.DRAFT)
                .build();
    }

    private PgTenancy buildApartmentTenancy() {
        return PgTenancy.builder()
                .id(UUID.randomUUID())
                .tenancyRef("PGT-2026-0002")
                .tenantId(UUID.randomUUID())
                .listingId(UUID.randomUUID())
                .sharingType("PRIVATE")
                .moveInDate(LocalDate.of(2026, 4, 15))
                .noticePeriodDays(60)
                .monthlyRentPaise(3500000)
                .securityDepositPaise(10000000)
                .totalMonthlyPaise(3500000)
                .mealsIncluded(false)
                .laundryIncluded(false)
                .wifiIncluded(false)
                .billingDay(15)
                .gracePeriodDays(5)
                .latePenaltyBps(100)
                .maxPenaltyPercent(20)
                .status(TenancyStatus.ACTIVE)
                .build();
    }

    private TenancyAgreement buildApartmentAgreement() {
        return TenancyAgreement.builder()
                .id(UUID.randomUUID())
                .tenancyId(UUID.randomUUID())
                .agreementNumber("AGR-2026-0002")
                .tenantName("Priya Patel")
                .tenantPhone("+919876500000")
                .tenantEmail("priya.patel@company.com")
                .tenantAadhaarLast4("4567")
                .hostName("Suresh Kumar")
                .hostPhone("+919999900001")
                .propertyAddress("301, Skylark Residency, 12th Main, Indiranagar, Bangalore - 560038")
                .roomDescription("2BHK - Full apartment (Flat 301)")
                .moveInDate(LocalDate.of(2026, 4, 15))
                .lockInPeriodMonths(11)
                .noticePeriodDays(60)
                .monthlyRentPaise(3500000)
                .securityDepositPaise(10000000)
                .maintenanceChargesPaise(350000)
                .termsAndConditions("No structural modifications. Tenant responsible for utility bills.")
                .status(AgreementStatus.DRAFT)
                .build();
    }
}
