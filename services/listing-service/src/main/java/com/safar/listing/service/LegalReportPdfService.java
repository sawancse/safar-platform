package com.safar.listing.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.safar.listing.entity.LegalCase;
import com.safar.listing.entity.LegalVerification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class LegalReportPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    public byte[] generatePdf(LegalCase legalCase, List<LegalVerification> verifications) {
        String html = renderHtml(legalCase, verifications);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();

            log.info("Legal report PDF generated for case {}: {} bytes", legalCase.getId(), os.size());
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate legal report PDF for case {}: {}", legalCase.getId(), e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private String renderHtml(LegalCase legalCase, List<LegalVerification> verifications) {
        String riskColor = legalCase.getRiskLevel() == null ? "#6b7280" : switch (legalCase.getRiskLevel()) {
            case GREEN -> "#16a34a";
            case YELLOW -> "#ca8a04";
            case RED -> "#dc2626";
        };

        StringBuilder verRows = new StringBuilder();
        for (LegalVerification v : verifications) {
            String statusColor = v.getStatus() == null ? "#6b7280" : switch (v.getStatus()) {
                case CLEAN -> "#16a34a";
                case ISSUE_FOUND -> "#dc2626";
                default -> "#6b7280";
            };
            verRows.append("<tr>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #e5e7eb;\">").append(formatEnum(v.getVerificationType().name())).append("</td>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #e5e7eb;color:").append(statusColor).append(";\">").append(formatEnum(v.getStatus().name())).append("</td>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #e5e7eb;\">").append(v.getFindings() != null ? escapeHtml(v.getFindings()) : "-").append("</td>")
                    .append("<td style=\"padding:8px 12px;border-bottom:1px solid #e5e7eb;\">").append(v.getRecommendation() != null ? escapeHtml(v.getRecommendation()) : "-").append("</td>")
                    .append("</tr>");
        }

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Helvetica, Arial, sans-serif; color: #1f2937; margin: 40px; font-size: 13px; }
                        h1 { color: #111827; font-size: 22px; margin-bottom: 4px; }
                        .subtitle { color: #6b7280; font-size: 13px; margin-bottom: 24px; }
                        .section { margin-bottom: 24px; }
                        .section-title { font-size: 15px; font-weight: bold; color: #374151; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px; margin-bottom: 12px; }
                        table { width: 100%%; border-collapse: collapse; }
                        th { text-align: left; padding: 8px 12px; background: #f9fafb; border-bottom: 2px solid #e5e7eb; font-size: 12px; color: #6b7280; text-transform: uppercase; }
                        .badge { display: inline-block; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: bold; color: white; }
                        .info-grid { display: flex; flex-wrap: wrap; }
                        .info-item { width: 48%%; margin-bottom: 8px; }
                        .info-label { font-size: 11px; color: #6b7280; text-transform: uppercase; }
                        .info-value { font-size: 13px; color: #111827; }
                        .summary-box { background: #f9fafb; border: 1px solid #e5e7eb; border-radius: 8px; padding: 16px; white-space: pre-wrap; font-size: 12px; }
                        .footer { margin-top: 40px; padding-top: 16px; border-top: 1px solid #e5e7eb; font-size: 11px; color: #9ca3af; text-align: center; }
                    </style>
                </head>
                <body>
                    <h1>Legal Verification Report</h1>
                    <div class="subtitle">Case ID: %s</div>

                    <div class="section">
                        <div class="section-title">Property Details</div>
                        <table>
                            <tr><td style="padding:4px 0;width:140px;color:#6b7280;">Address</td><td>%s</td></tr>
                            <tr><td style="padding:4px 0;color:#6b7280;">City</td><td>%s</td></tr>
                            <tr><td style="padding:4px 0;color:#6b7280;">State</td><td>%s</td></tr>
                            <tr><td style="padding:4px 0;color:#6b7280;">Package</td><td>%s</td></tr>
                            <tr><td style="padding:4px 0;color:#6b7280;">Status</td><td>%s</td></tr>
                            <tr><td style="padding:4px 0;color:#6b7280;">Risk Level</td><td><span class="badge" style="background:%s;">%s</span></td></tr>
                            <tr><td style="padding:4px 0;color:#6b7280;">Report Date</td><td>%s</td></tr>
                        </table>
                    </div>

                    <div class="section">
                        <div class="section-title">Verifications (%d)</div>
                        <table>
                            <tr><th>Type</th><th>Status</th><th>Findings</th><th>Recommendation</th></tr>
                            %s
                        </table>
                    </div>

                    <div class="section">
                        <div class="section-title">Summary</div>
                        <div class="summary-box">%s</div>
                    </div>

                    <div class="footer">
                        Generated by Safar Legal Services &#8226; This report is for informational purposes only and does not constitute legal advice.
                    </div>
                </body>
                </html>
                """.formatted(
                legalCase.getId(),
                escapeHtml(legalCase.getPropertyAddress() != null ? legalCase.getPropertyAddress() : "-"),
                escapeHtml(legalCase.getCity() != null ? legalCase.getCity() : "-"),
                escapeHtml(legalCase.getState() != null ? legalCase.getState() : "-"),
                formatEnum(legalCase.getPackageType().name()),
                formatEnum(legalCase.getStatus().name()),
                riskColor,
                legalCase.getRiskLevel() != null ? legalCase.getRiskLevel().name() : "PENDING",
                legalCase.getReportReadyAt() != null ? legalCase.getReportReadyAt().format(DATE_FMT) : "-",
                verifications.size(),
                verRows.toString(),
                escapeHtml(legalCase.getReportSummary() != null ? legalCase.getReportSummary() : "No summary available.")
        );
    }

    private String formatEnum(String enumName) {
        return enumName.replace("_", " ").substring(0, 1).toUpperCase()
                + enumName.replace("_", " ").substring(1).toLowerCase();
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
