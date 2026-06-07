package com.safar.listing.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import com.safar.listing.entity.AgreementParty;
import com.safar.listing.entity.AgreementRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Renders a draft agreement to PDF (openhtmltopdf, same engine as the legal
 * report). The draft is generated on the fly from the stored agreement data —
 * no file is persisted; the GET /document/draft.pdf endpoint streams the bytes.
 */
@Service
@Slf4j
@lombok.RequiredArgsConstructor
public class AgreementPdfService {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

    public byte[] generateDraftPdf(AgreementRequest a, List<AgreementParty> parties) {
        String html = renderHtml(a, parties);
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            log.info("Agreement draft PDF generated for {}: {} bytes", a.getId(), os.size());
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate agreement draft PDF for {}: {}", a.getId(), e.getMessage(), e);
            throw new RuntimeException("PDF generation failed: " + e.getMessage(), e);
        }
    }

    private String renderHtml(AgreementRequest a, List<AgreementParty> parties) {
        StringBuilder partyRows = new StringBuilder();
        if (parties != null) {
            for (AgreementParty p : parties) {
                partyRows.append(partyRow(
                        p.getPartyType() != null ? formatEnum(p.getPartyType().name()) : "-",
                        p.getFullName(), p.getPhone(), p.getEmail(), p.getPanNumber()));
            }
        }
        // The create wizard stores party details on termsJson as a JSON array
        // (name/role/phone/email/panNumber), not as separate AgreementParty rows —
        // render those so the PDF isn't blank when no rows were explicitly added.
        if (partyRows.length() == 0 && a.getTermsJson() != null && !a.getTermsJson().isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(a.getTermsJson());
                if (arr.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                        String role = text(n, "role");
                        partyRows.append(partyRow(
                                role != null ? formatEnum(role) : "-",
                                text(n, "name"), text(n, "phone"), text(n, "email"), text(n, "panNumber")));
                    }
                }
            } catch (Exception e) {
                log.warn("Could not parse termsJson parties for agreement {}: {}", a.getId(), e.getMessage());
            }
        }
        if (partyRows.length() == 0) {
            partyRows.append("<tr><td colspan=\"5\" style=\"padding:8px 12px;color:#9ca3af;\">No parties added yet.</td></tr>");
        }

        StringBuilder terms = new StringBuilder();
        addTerm(terms, "Agreement date", a.getAgreementDate() != null ? a.getAgreementDate().format(DATE_FMT) : null);
        addTerm(terms, "Start date", a.getStartDate() != null ? a.getStartDate().format(DATE_FMT) : null);
        addTerm(terms, "End date", a.getEndDate() != null ? a.getEndDate().format(DATE_FMT) : null);
        addTerm(terms, "Monthly rent", money(a.getMonthlyRentPaise()));
        addTerm(terms, "Security deposit", money(a.getSecurityDepositPaise()));
        addTerm(terms, "Sale consideration", money(a.getSaleConsiderationPaise()));
        if (terms.length() == 0) {
            terms.append("<tr><td colspan=\"2\" style=\"padding:6px 0;color:#9ca3af;\">No terms captured.</td></tr>");
        }

        String clauses = renderClauses(a.getClausesJson());

        return """
                <!DOCTYPE html>
                <html>
                <head>
                    <style>
                        body { font-family: Helvetica, Arial, sans-serif; color: #1f2937; margin: 40px; font-size: 13px; }
                        h1 { color: #111827; font-size: 22px; margin-bottom: 2px; }
                        .subtitle { color: #6b7280; font-size: 12px; margin-bottom: 4px; }
                        .draft { display:inline-block; margin-top:6px; padding:3px 10px; border-radius:12px; font-size:11px; font-weight:bold; color:#92400e; background:#fef3c7; letter-spacing:1px; }
                        .section { margin-bottom: 22px; }
                        .section-title { font-size: 14px; font-weight: bold; color: #374151; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px; margin-bottom: 10px; }
                        table { width: 100%%; border-collapse: collapse; }
                        th { text-align: left; padding: 8px 12px; background: #f9fafb; border-bottom: 2px solid #e5e7eb; font-size: 11px; color: #6b7280; text-transform: uppercase; }
                        td { font-size: 12px; }
                        .meta td { padding: 4px 0; }
                        .meta td.k { width: 150px; color: #6b7280; }
                        .fees td { padding: 6px 0; border-bottom: 1px dashed #e5e7eb; }
                        .fees td.k { color: #6b7280; }
                        .fees td.v { text-align: right; font-weight: 600; }
                        .total td { border-top: 2px solid #e5e7eb; border-bottom: none; font-weight: bold; padding-top: 8px; }
                        .clause { margin-bottom: 8px; font-size: 12px; }
                        .footer { margin-top: 36px; padding-top: 14px; border-top: 1px solid #e5e7eb; font-size: 10px; color: #9ca3af; text-align: center; }
                    </style>
                </head>
                <body>
                    <h1>%s</h1>
                    <div class="subtitle">Ref: %s</div>
                    <div class="draft">DRAFT &#8226; UNSIGNED</div>

                    <div class="section" style="margin-top:18px;">
                        <table class="meta">
                            <tr><td class="k">State</td><td>%s</td></tr>
                            <tr><td class="k">City</td><td>%s</td></tr>
                            <tr><td class="k">Package</td><td>%s</td></tr>
                            <tr><td class="k">Status</td><td>%s</td></tr>
                            <tr><td class="k">Created</td><td>%s</td></tr>
                        </table>
                    </div>

                    <div class="section">
                        <div class="section-title">Parties</div>
                        <table>
                            <tr><th>Role</th><th>Name</th><th>Phone</th><th>Email</th><th>PAN</th></tr>
                            %s
                        </table>
                    </div>

                    <div class="section">
                        <div class="section-title">Terms</div>
                        <table class="meta">%s</table>
                    </div>

                    <div class="section">
                        <div class="section-title">Clauses</div>
                        %s
                    </div>

                    <div class="section">
                        <div class="section-title">Fees &amp; Charges</div>
                        <table class="fees">
                            <tr><td class="k">Stamp duty</td><td class="v">%s</td></tr>
                            <tr><td class="k">Registration fee</td><td class="v">%s</td></tr>
                            <tr><td class="k">Service fee</td><td class="v">%s</td></tr>
                            <tr class="total"><td class="k">Total</td><td class="v">%s</td></tr>
                        </table>
                    </div>

                    <div class="footer">
                        Generated by Safar &#8226; This is an unsigned draft for review only and does not constitute a legally executed agreement.
                    </div>
                </body>
                </html>
                """.formatted(
                escapeHtml(typeTitle(a)),
                a.getId(),
                escapeHtml(orDash(a.getState())),
                escapeHtml(orDash(a.getCity())),
                a.getAgreementPackage() != null ? formatEnum(a.getAgreementPackage().name()) : "-",
                a.getStatus() != null ? formatEnum(a.getStatus().name()) : "-",
                a.getCreatedAt() != null ? a.getCreatedAt().format(DATE_FMT) : "-",
                partyRows.toString(),
                terms.toString(),
                clauses,
                money(a.getStampDutyPaise()),
                money(a.getRegistrationFeePaise()),
                money(a.getServiceFeePaise()),
                money(a.getTotalFeePaise())
        );
    }

    private String typeTitle(AgreementRequest a) {
        return a.getAgreementType() != null ? formatEnum(a.getAgreementType().name()) : "Agreement";
    }

    /** clausesJson may be a JSON array of strings, a JSON object, or plain text. Render best-effort. */
    private String renderClauses(String clausesJson) {
        if (clausesJson == null || clausesJson.isBlank()) {
            return "<div class=\"clause\" style=\"color:#9ca3af;\">Standard clauses for this agreement type will be included on execution.</div>";
        }
        String t = clausesJson.trim();
        StringBuilder sb = new StringBuilder();
        if (t.startsWith("[")) {
            // crude split of a JSON string array — avoids pulling a JSON parser here
            String inner = t.substring(1, Math.max(1, t.length() - 1));
            String[] items = inner.split("\",\\s*\"");
            int n = 1;
            for (String it : items) {
                String clean = it.replaceAll("^\\s*\"|\"\\s*$", "").trim();
                if (!clean.isEmpty()) {
                    sb.append("<div class=\"clause\">").append(n++).append(". ").append(escapeHtml(clean)).append("</div>");
                }
            }
        }
        if (sb.length() == 0) {
            sb.append("<div class=\"clause\" style=\"white-space:pre-wrap;\">").append(escapeHtml(t)).append("</div>");
        }
        return sb.toString();
    }

    private void addTerm(StringBuilder sb, String label, String value) {
        if (value != null) {
            sb.append("<tr><td class=\"k\">").append(escapeHtml(label)).append("</td><td>").append(escapeHtml(value)).append("</td></tr>");
        }
    }

    private String td(String v) {
        return "<td style=\"padding:8px 12px;border-bottom:1px solid #e5e7eb;\">" + escapeHtml(orDash(v)) + "</td>";
    }

    private String partyRow(String role, String name, String phone, String email, String pan) {
        return "<tr>" + td(role) + td(name) + td(phone) + td(email) + td(pan) + "</tr>";
    }

    private String text(com.fasterxml.jackson.databind.JsonNode n, String field) {
        return n.has(field) && !n.get(field).isNull() ? n.get(field).asText() : null;
    }

    private String money(Long paise) {
        if (paise == null) return null;
        return "₹" + String.format(Locale.ENGLISH, "%,d", paise / 100);
    }

    private String orDash(String s) {
        return (s == null || s.isBlank()) ? "-" : s;
    }

    private String formatEnum(String enumName) {
        String s = enumName.replace("_", " ").toLowerCase(Locale.ENGLISH);
        return s.substring(0, 1).toUpperCase(Locale.ENGLISH) + s.substring(1);
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
