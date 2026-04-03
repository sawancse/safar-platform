package com.safar.booking.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TenancyAgreement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementPdfService {

    private final TemplateEngine templateEngine;
    private final PgTenancyService tenancyService;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a", Locale.ENGLISH);

    /**
     * Generate PDF bytes for a tenancy agreement.
     */
    public byte[] generatePdf(TenancyAgreement agreement) {
        PgTenancy tenancy = tenancyService.getTenancy(agreement.getTenancyId());
        String html = renderHtml(agreement, tenancy);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();

            log.info("PDF generated for agreement {}: {} bytes", agreement.getAgreementNumber(), os.size());
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for agreement {}: {}", agreement.getAgreementNumber(), e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Render the agreement as an HTML string (for browser viewing).
     */
    public String renderHtml(TenancyAgreement agreement, PgTenancy tenancy) {
        Context ctx = new Context();

        // Agreement fields
        ctx.setVariable("agreementNumber", agreement.getAgreementNumber());
        ctx.setVariable("moveInDate", agreement.getMoveInDate().format(DATE_FMT));
        ctx.setVariable("hostName", agreement.getHostName());
        ctx.setVariable("hostPhone", agreement.getHostPhone() != null ? agreement.getHostPhone() : "N/A");
        ctx.setVariable("propertyAddress", agreement.getPropertyAddress());
        ctx.setVariable("tenantName", agreement.getTenantName());
        ctx.setVariable("tenantPhone", agreement.getTenantPhone() != null ? agreement.getTenantPhone() : "N/A");
        ctx.setVariable("tenantEmail", agreement.getTenantEmail() != null ? agreement.getTenantEmail() : "N/A");
        ctx.setVariable("aadhaarLast4", agreement.getTenantAadhaarLast4());
        ctx.setVariable("roomDescription", agreement.getRoomDescription());
        ctx.setVariable("sharingType", tenancy.getSharingType());

        // Duration
        ctx.setVariable("lockInMonths", agreement.getLockInPeriodMonths());
        ctx.setVariable("noticeDays", agreement.getNoticePeriodDays());

        // Financial
        ctx.setVariable("rentFormatted", formatInr(agreement.getMonthlyRentPaise()));
        ctx.setVariable("maintenanceFormatted", formatInr(agreement.getMaintenanceChargesPaise()));
        ctx.setVariable("totalFormatted", formatInr(agreement.getMonthlyRentPaise() + agreement.getMaintenanceChargesPaise()));
        ctx.setVariable("depositFormatted", formatInr(agreement.getSecurityDepositPaise()));

        // Penalty config
        ctx.setVariable("billingDayOrdinal", ordinalSuffix(tenancy.getBillingDay()));
        ctx.setVariable("gracePeriodDays", tenancy.getGracePeriodDays());
        ctx.setVariable("penaltyPercent", String.format("%.1f%%", tenancy.getLatePenaltyBps() / 100.0));
        ctx.setVariable("maxPenaltyPercent", tenancy.getMaxPenaltyPercent() + "%");

        // Inclusions
        ctx.setVariable("mealsIncluded", tenancy.isMealsIncluded());
        ctx.setVariable("laundryIncluded", tenancy.isLaundryIncluded());
        ctx.setVariable("wifiIncluded", tenancy.isWifiIncluded());

        // Signatures
        ctx.setVariable("hostSignedAt", agreement.getHostSignedAt() != null
                ? agreement.getHostSignedAt().format(DATETIME_FMT) : null);
        ctx.setVariable("hostSignatureIp", agreement.getHostSignatureIp());
        ctx.setVariable("tenantSignedAt", agreement.getTenantSignedAt() != null
                ? agreement.getTenantSignedAt().format(DATETIME_FMT) : null);
        ctx.setVariable("tenantSignatureIp", agreement.getTenantSignatureIp());

        // Additional terms
        ctx.setVariable("additionalTerms", agreement.getTermsAndConditions());

        return templateEngine.process("agreement-pdf", ctx);
    }

    private String formatInr(long paise) {
        long inr = paise / 100;
        return String.format("\u20B9%,d", inr);
    }

    private String ordinalSuffix(int day) {
        if (day >= 11 && day <= 13) return day + "th";
        return switch (day % 10) {
            case 1 -> day + "st";
            case 2 -> day + "nd";
            case 3 -> day + "rd";
            default -> day + "th";
        };
    }
}
