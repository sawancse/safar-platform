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
        String html = isSaleAgreement(a) ? renderSaleAgreementHtml(a, parties) : renderHtml(a, parties);
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

    /**
     * clausesJson is typically the create wizard's object form
     * {"clauses":["indemnity",...],"customClause":"..."} — but may also be a bare
     * JSON array or plain text. Render the clause keys human-readably.
     */
    private String renderClauses(String clausesJson) {
        if (clausesJson == null || clausesJson.isBlank()) {
            return "<div class=\"clause\" style=\"color:#9ca3af;\">Standard clauses for this agreement type will be included on execution.</div>";
        }
        StringBuilder sb = new StringBuilder();
        int[] n = { 1 };
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(clausesJson);
            com.fasterxml.jackson.databind.JsonNode arr = node.isArray() ? node : node.get("clauses");
            if (arr != null && arr.isArray()) {
                for (com.fasterxml.jackson.databind.JsonNode c : arr) {
                    String key = c.asText("");
                    if (!key.isBlank()) {
                        sb.append("<div class=\"clause\">").append(n[0]++).append(". ").append(escapeHtml(humanizeClause(key))).append("</div>");
                    }
                }
            }
            com.fasterxml.jackson.databind.JsonNode custom = node.isObject() ? node.get("customClause") : null;
            if (custom != null && !custom.isNull() && !custom.asText("").isBlank()) {
                sb.append("<div class=\"clause\">").append(n[0]++).append(". ").append(escapeHtml(custom.asText())).append("</div>");
            }
        } catch (Exception e) {
            // Not JSON — render as plain text.
            sb.append("<div class=\"clause\" style=\"white-space:pre-wrap;\">").append(escapeHtml(clausesJson.trim())).append("</div>");
        }
        if (sb.length() == 0) {
            return "<div class=\"clause\" style=\"color:#9ca3af;\">Standard clauses for this agreement type will be included on execution.</div>";
        }
        return sb.toString();
    }

    /** camelCase / snake_case clause key → Title Case, e.g. "disputeResolution" → "Dispute Resolution". */
    private String humanizeClause(String key) {
        if (key == null || key.isBlank()) return "";
        String spaced = key.replaceAll("([a-z])([A-Z])", "$1 $2").replace("_", " ").trim();
        return spaced.substring(0, 1).toUpperCase(Locale.ENGLISH) + spaced.substring(1);
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

    // ════════════════════════ RERA "Agreement for Sale" (builder/seller ↔ buyer) ════════════════════════

    private boolean isSaleAgreement(AgreementRequest a) {
        return a.getAgreementType() == com.safar.listing.entity.enums.AgreementType.SALE_AGREEMENT
                || a.getAgreementType() == com.safar.listing.entity.enums.AgreementType.SALE_DEED;
    }

    /** [role, name, phone, email, pan, address] for each party — AgreementParty rows, else termsJson. */
    private java.util.List<String[]> collectParties(AgreementRequest a, java.util.List<AgreementParty> rows) {
        java.util.List<String[]> out = new java.util.ArrayList<>();
        if (rows != null) {
            for (AgreementParty p : rows) {
                out.add(new String[]{
                        p.getPartyType() != null ? p.getPartyType().name() : "",
                        nz(p.getFullName()), nz(p.getPhone()), nz(p.getEmail()), nz(p.getPanNumber()), nz(p.getAddress())});
            }
        }
        if (out.isEmpty() && a.getTermsJson() != null && !a.getTermsJson().isBlank()) {
            try {
                com.fasterxml.jackson.databind.JsonNode arr = objectMapper.readTree(a.getTermsJson());
                if (arr.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode n : arr) {
                        out.add(new String[]{
                                nz(text(n, "role")), nz(text(n, "name")), nz(text(n, "phone")),
                                nz(text(n, "email")), nz(text(n, "panNumber")), nz(text(n, "address"))});
                    }
                }
            } catch (Exception ignored) { }
        }
        return out;
    }

    private String nz(String s) { return s == null ? "" : s; }
    private boolean roleIn(String role, String... wanted) {
        for (String w : wanted) if (w.equalsIgnoreCase(role)) return true;
        return false;
    }

    private String renderSaleAgreementHtml(AgreementRequest a, java.util.List<AgreementParty> rows) {
        java.util.List<String[]> parties = collectParties(a, rows);

        StringBuilder promoter = new StringBuilder();
        StringBuilder allottee = new StringBuilder();
        StringBuilder allotteeSig = new StringBuilder();
        StringBuilder witnessSig = new StringBuilder();
        int allotteeNo = 1, witnessNo = 1;
        for (String[] p : parties) {
            String role = p[0], name = p[1], pan = p[4], addr = p[5];
            String line = "<b>" + escapeHtml(blank(name)) + "</b> (PAN " + escapeHtml(blank(pan)) + "), "
                    + (addr.isBlank() ? "residing at ________________" : "residing at " + escapeHtml(addr));
            if (roleIn(role, "SELLER", "PROMOTER")) {
                if (promoter.length() > 0) promoter.append("<br/>");
                promoter.append(line);
            } else if (roleIn(role, "WITNESS")) {
                witnessSig.append(sigItem(witnessNo++, name, addr));
            } else { // BUYER / ALLOTTEE / anything else
                if (allottee.length() > 0) allottee.append("<br/>");
                allottee.append(line);
                allotteeSig.append(sigItem(allotteeNo++, name, addr));
            }
        }
        if (promoter.length() == 0) promoter.append("<b>________________________</b> (PAN ___________), the Promoter/Seller");
        if (allottee.length() == 0) allottee.append("<b>________________________</b> (PAN ___________), the Allottee/Buyer");
        if (allotteeSig.length() == 0) allotteeSig.append(sigItem(1, "", ""));
        if (witnessSig.length() == 0) { witnessSig.append(sigItem(1, "", "")).append(sigItem(2, "", "")); }

        String price = a.getSaleConsiderationPaise() != null && a.getSaleConsiderationPaise() > 0
                ? money(a.getSaleConsiderationPaise())
                : (a.getTotalFeePaise() != null && a.getTotalFeePaise() > 0 ? money(a.getTotalFeePaise()) : "Rs. ____________________");
        String execDate = a.getAgreementDate() != null ? a.getAgreementDate().format(DATE_FMT) : "____ day of __________, 20____";

        return SALE_TEMPLATE
                .replace("{{REF}}", String.valueOf(a.getId()))
                .replace("{{EXEC_DATE}}", escapeHtml(execDate))
                .replace("{{PROMOTER_BLOCK}}", promoter.toString())
                .replace("{{ALLOTTEE_BLOCK}}", allottee.toString())
                .replace("{{TOTAL_PRICE}}", escapeHtml(price))
                .replace("{{CITY}}", escapeHtml(orDash(a.getCity())))
                .replace("{{STATE}}", escapeHtml(orDash(a.getState())))
                .replace("{{SIGN_ALLOTTEE}}", allotteeSig.toString())
                .replace("{{SIGN_WITNESS}}", witnessSig.toString());
    }

    private String blank(String s) { return (s == null || s.isBlank()) ? "________________" : s; }

    private String sigItem(int i, String name, String addr) {
        return "<div style=\"margin:10px 0;\">(" + i + ") Signature ____________________________<br/>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;Name " + escapeHtml(blank(name)) + "<br/>"
                + "&nbsp;&nbsp;&nbsp;&nbsp;Address " + escapeHtml(addr.isBlank() ? "____________________________" : addr) + "</div>";
    }

    /** Standard RERA model Agreement for Sale. {{TOKENS}} are merged from the booking's data. */
    private static final String SALE_TEMPLATE = """
            <!DOCTYPE html><html><head><style>
              body { font-family: 'Times New Roman', Georgia, serif; color:#111; font-size:11.5px; line-height:1.5; margin:36px; }
              h1 { text-align:center; font-size:18px; margin:0 0 4px; letter-spacing:1px; }
              .draft { text-align:center; color:#92400e; font-weight:bold; font-size:10px; letter-spacing:2px; margin-bottom:14px; }
              h2 { font-size:12.5px; margin:16px 0 4px; }
              ol.clauses { padding-left:18px; }
              ol.clauses > li { margin:10px 0; }
              ol.lower-roman { list-style-type: lower-roman; padding-left:18px; }
              ol.lower-alpha { list-style-type: lower-alpha; padding-left:18px; }
              p { margin:8px 0; text-align:justify; }
              table { width:100%; border-collapse:collapse; margin:8px 0; }
              td, th { border:1px solid #555; padding:5px 8px; font-size:11px; text-align:left; }
              .pagebreak { page-break-before: always; }
              .sigwrap { display:flex; justify-content:space-between; }
              .center { text-align:center; }
            </style></head><body>

            <h1>AGREEMENT FOR SALE</h1>
            <div class="draft">DRAFT &bull; UNSIGNED &bull; Ref {{REF}}</div>

            <p>This Agreement for Sale (&ldquo;Agreement&rdquo;) is executed on this {{EXEC_DATE}}, at {{CITY}}, {{STATE}},</p>
            <p><b>By and Between</b></p>
            <p>{{PROMOTER_BLOCK}}, hereinafter referred to as the &ldquo;Promoter&rdquo; (which expression shall, unless repugnant to the context, include its successors and permitted assigns);</p>
            <p><b>AND</b></p>
            <p>{{ALLOTTEE_BLOCK}}, hereinafter referred to as the &ldquo;Allottee&rdquo; (which expression shall, unless repugnant to the context, include his/her/their heirs, executors and permitted assigns).</p>
            <p>The Promoter and the Allottee shall hereinafter collectively be referred to as the &ldquo;Parties&rdquo; and individually as a &ldquo;Party&rdquo;.</p>

            <h2>WHEREAS:</h2>
            <ol class="lower-alpha">
              <li>The Promoter is the absolute and lawful owner of / is fully competent to deal with the land more particularly described in Schedule A (the &ldquo;Said Land&rdquo;), free from all reasonable doubts and encumbrances, save as disclosed herein.</li>
              <li>The Said Land is earmarked for the purpose of developing a project (the &ldquo;Project&rdquo;), and the Promoter has obtained the requisite layout/sanctioned plans, specifications and approvals for the Project from the competent authority.</li>
              <li>The Promoter has, where applicable, registered the Project under the Real Estate (Regulation and Development) Act, 2016 with the concerned Real Estate Regulatory Authority.</li>
              <li>The Allottee has applied for, and the Promoter has agreed to sell, the apartment/plot/building unit more particularly described in Schedule A together with the proportionate undivided share in the common areas (the &ldquo;Apartment/Unit&rdquo;).</li>
              <li>The Parties have gone through all the terms and conditions set out in this Agreement and understood the mutual rights and obligations detailed herein, and are now willing to enter into this Agreement on the terms and conditions appearing hereinafter.</li>
            </ol>

            <p><b>NOW THEREFORE</b>, in consideration of the mutual representations, covenants, assurances, promises and agreements contained herein and other good and valuable consideration, the Parties agree as follows:</p>

            <ol class="clauses">
              <li><b>TERMS:</b> Subject to the terms of this Agreement, the Promoter agrees to sell to the Allottee and the Allottee agrees to purchase the Apartment/Unit described in Schedule A. The Total Price for the Apartment/Unit, based on the carpet area, is <b>{{TOTAL_PRICE}}</b> (Rupees ________________________________ only), excluding applicable GST, stamp duty and registration charges. A breakup of the price (cost of unit, exclusive/balcony/terrace areas, proportionate common areas, preferential location charges, taxes and maintenance, as applicable) is set out in the price schedule. The Total Price is escalation-free save and except increases on account of development charges payable to the competent authority and/or statutory increases. The Allottee shall make payment as per the Payment Plan in Schedule C.</li>
              <li><b>MODE OF PAYMENT:</b> Subject to the terms of this Agreement and the Promoter abiding by the construction milestones, the Allottee shall make all payments, on written demand by the Promoter, within the stipulated time as per the Payment Plan [Schedule C], through A/c Payee cheque / demand draft / banker&rsquo;s cheque / online transfer in favour of the Promoter, payable at {{CITY}}.</li>
              <li><b>COMPLIANCE OF LAWS RELATING TO REMITTANCES:</b> The Allottee, if a person resident outside India, shall be solely responsible for complying with the Foreign Exchange Management Act, 1999, the rules and regulations of the Reserve Bank of India and all other applicable laws, and shall keep the Promoter indemnified and harmless in this regard.</li>
              <li><b>ADJUSTMENT / APPROPRIATION OF PAYMENTS:</b> The Allottee authorises the Promoter to adjust/appropriate all payments made under any head(s) of dues against lawful outstandings of the Allottee in respect of the Apartment/Unit.</li>
              <li><b>TIME IS ESSENCE:</b> The Promoter shall abide by the time schedule for completing the Project and handing over the Apartment/Unit to the Allottee and the common areas to the association of allottees or the competent authority, as the case may be.</li>
              <li><b>CONSTRUCTION OF THE PROJECT / APARTMENT:</b> The Allottee has seen and accepted the proposed layout, floor plan, payment plan and the specifications, amenities and facilities approved by the competent authority. The Promoter shall develop the Project in accordance with such approved plans and shall not make any variation/alteration except in the manner provided under the Act; breach of this term shall constitute a material breach.</li>
              <li><b>POSSESSION OF THE APARTMENT/UNIT:</b> The Promoter assures to hand over possession of the Apartment/Unit by ____________, subject to Force Majeure. Upon obtaining the occupancy/completion certificate, the Promoter shall offer possession in writing, to be taken by the Allottee within the time specified. On any delay not attributable to Force Majeure, the Promoter shall be liable to pay interest at the prescribed rate or, at the Allottee&rsquo;s option, refund the amounts received with interest, as provided under the Act.</li>
              <li><b>REPRESENTATIONS AND WARRANTIES OF THE PROMOTER:</b> The Promoter represents and warrants that it has absolute, clear and marketable title and the requisite rights and approvals to develop the Project; that there are no undisclosed encumbrances or litigation in respect of the Said Land/Project/Apartment; and that it has the right to enter into this Agreement and to sell the Apartment/Unit to the Allottee.</li>
              <li><b>EVENTS OF DEFAULTS AND CONSEQUENCES:</b> The consequences of default by either Party, including the Allottee&rsquo;s remedies on the Promoter&rsquo;s default and the Promoter&rsquo;s remedies on the Allottee&rsquo;s default (including liability to pay interest at the prescribed rate and, after due notice, cancellation/refund after deducting the booking amount), shall be governed by the provisions of the Act and the Rules.</li>
              <li><b>CONVEYANCE OF THE SAID APARTMENT/UNIT:</b> The Promoter, on receipt of the Total Price, shall execute a registered conveyance deed conveying the title of the Apartment/Unit together with the proportionate undivided share in the common areas, within the period prescribed under the Act. The Allottee shall bear the stamp duty and registration charges.</li>
              <li><b>MAINTENANCE OF THE SAID BUILDING / APARTMENT / PROJECT:</b> The Promoter shall be responsible to provide and maintain essential services in the Project, on payment of maintenance charges, until the maintenance is taken over by the association of allottees.</li>
              <li><b>DEFECT LIABILITY:</b> Any structural defect or defect in workmanship, quality or provision of services brought to the notice of the Promoter within five (5) years from the date of handing over possession shall be rectified by the Promoter, without further charge, within thirty (30) days, failing which the Allottee shall be entitled to compensation as provided under the Act.</li>
              <li><b>RIGHT TO ENTER THE APARTMENT FOR REPAIRS:</b> The Promoter / maintenance agency / association of allottees shall have the right of access to the Apartment/Unit and common areas, after due notice, for maintenance and to set right any defect.</li>
              <li><b>USAGE:</b> Basements and service areas shall be used only for their earmarked purposes as per the sanctioned plans; the Allottee shall not use such areas otherwise.</li>
              <li><b>GENERAL COMPLIANCE:</b> After taking possession, the Allottee shall maintain the Apartment/Unit at his/her own cost and shall not carry out any structural change, alter the external elevation/colour scheme, place signage on the façade, store hazardous goods, or remove any load-bearing wall.</li>
              <li><b>COMPLIANCE OF LAWS, NOTIFICATIONS ETC. BY PARTIES:</b> The Parties enter into this Agreement with full knowledge of all laws, rules, regulations and notifications applicable to the Project.</li>
              <li><b>ADDITIONAL CONSTRUCTIONS:</b> The Promoter shall not make any additions or put up additional structures in the Project after the plans and specifications have been approved and disclosed, except as provided under the Act.</li>
              <li><b>PROMOTER SHALL NOT MORTGAGE OR CREATE A CHARGE:</b> After execution of this Agreement, the Promoter shall not mortgage or create any charge on the Apartment/Unit, and any such mortgage/charge shall not affect the right and interest of the Allottee.</li>
              <li><b>APARTMENT OWNERSHIP ACT:</b> The Project is in accordance with the provisions of the relevant State apartment ownership law.</li>
              <li><b>BINDING EFFECT:</b> This Agreement shall be binding only upon execution and delivery by the Allottee, along with payments due as per the Payment Plan, within thirty (30) days of receipt, and appearance for registration before the concerned Sub-Registrar when intimated by the Promoter.</li>
              <li><b>ENTIRE AGREEMENT:</b> This Agreement, along with its schedules, constitutes the entire agreement between the Parties and supersedes all prior understandings, allotment letters and correspondence.</li>
              <li><b>RIGHT TO AMEND:</b> This Agreement may only be amended through the written consent of the Parties.</li>
              <li><b>PROVISIONS APPLICABLE ON SUBSEQUENT ALLOTTEES:</b> All provisions and obligations herein shall equally apply to and be enforceable by/against any subsequent allottee of the Apartment/Unit.</li>
              <li><b>WAIVER NOT A LIMITATION TO ENFORCE:</b> Failure or delay by either Party to enforce any provision shall not be construed as a waiver of that or any other provision.</li>
              <li><b>SEVERABILITY:</b> If any provision is held void or unenforceable, it shall be deemed amended/deleted to the extent necessary to conform to the Act and the Rules, and the remaining provisions shall remain valid and enforceable.</li>
              <li><b>METHOD OF CALCULATION OF PROPORTIONATE SHARE:</b> Wherever the Allottee is required to make payment in common with other allottees, the same shall be in the proportion that the carpet area of the Apartment/Unit bears to the total carpet area of all units in the Project.</li>
              <li><b>FURTHER ASSURANCES:</b> Both Parties shall execute and deliver such further instruments and take such further actions as may be reasonably required to give effect to this Agreement.</li>
              <li><b>PLACE OF EXECUTION:</b> The execution of this Agreement shall be complete upon its execution by the Promoter and the Allottee at {{CITY}} and registration at the office of the concerned Sub-Registrar.</li>
              <li><b>NOTICES:</b> All notices shall be deemed duly served if sent by Registered Post / email to the Parties at their respective addresses recorded herein; each Party shall inform the other of any change of address.</li>
              <li><b>JOINT ALLOTTEES:</b> In case of joint allottees, all communications shall be sent to the allottee whose name appears first, which shall be deemed properly served on all the allottees.</li>
              <li><b>SAVINGS:</b> Any document signed by the Allottee prior to the execution and registration of this Agreement shall not limit the rights and interests of the Allottee under this Agreement or the Act.</li>
              <li><b>GOVERNING LAW:</b> The rights and obligations of the Parties shall be construed and enforced in accordance with the Real Estate (Regulation and Development) Act, 2016, the Rules and Regulations made thereunder, and other applicable laws of India.</li>
              <li><b>DISPUTE RESOLUTION:</b> All disputes arising out of or touching upon this Agreement shall be settled amicably by mutual discussion, failing which through the adjudicating officer appointed under the Act, before the courts at {{CITY}}, {{STATE}}.</li>
            </ol>

            <p style="margin-top:18px;"><b>IN WITNESS WHEREOF</b> the Parties have set their respective hands and signed this Agreement for Sale at {{CITY}}, in the presence of the attesting witnesses, on the day and year first above written.</p>

            <div class="sigwrap">
              <div style="width:48%;">
                <p><b>SIGNED AND DELIVERED by the Allottee:</b></p>
                {{SIGN_ALLOTTEE}}
              </div>
              <div style="width:48%;">
                <p><b>SIGNED AND DELIVERED by the Promoter:</b></p>
                <div style="margin:10px 0;">(1) Signature (Authorised Signatory) ____________<br/>
                &nbsp;&nbsp;&nbsp;&nbsp;Name ____________________________<br/>
                &nbsp;&nbsp;&nbsp;&nbsp;Address ____________________________</div>
              </div>
            </div>

            <p style="margin-top:10px;"><b>WITNESSES:</b></p>
            {{SIGN_WITNESS}}

            <div class="pagebreak"></div>
            <h2 class="center">SCHEDULE A &mdash; DESCRIPTION OF THE PROPERTY</h2>
            <table>
              <tr><td style="width:35%;"><b>Address of the property</b></td><td>____________________________, {{CITY}}, {{STATE}}</td></tr>
              <tr><td><b>Survey / Khasra no.</b></td><td>____________________________</td></tr>
              <tr><td><b>Carpet area</b></td><td>__________ sq. ft. / sq. mtr.</td></tr>
              <tr><td><b>Boundaries</b></td><td>North: __________ &nbsp; South: __________ &nbsp; East: __________ &nbsp; West: __________</td></tr>
            </table>
            <h2>SCHEDULE B &mdash; FLOOR PLAN OF THE APARTMENT/UNIT</h2>
            <p>[Floor plan to be annexed.]</p>

            <h2 class="center">SCHEDULE C &mdash; PAYMENT PLAN</h2>
            <table>
              <tr><th>Schedule for payment</th><th style="width:30%;">Payment percentage</th></tr>
              <tr><td>Booking amount</td><td>10%</td></tr>
              <tr><td>Within 15 days of booking</td><td>10%</td></tr>
              <tr><td>At the time of foundation</td><td>10%</td></tr>
              <tr><td>On completion of plinth</td><td>10%</td></tr>
              <tr><td>On casting of first floor slab</td><td>10%</td></tr>
              <tr><td>On casting of subsequent slabs</td><td>15%</td></tr>
              <tr><td>On completion of brick work</td><td>10%</td></tr>
              <tr><td>On completion of plaster</td><td>5%</td></tr>
              <tr><td>On completion of flooring</td><td>5%</td></tr>
              <tr><td>On possession</td><td>15%</td></tr>
            </table>

            <h2>SCHEDULE D &mdash; SPECIFICATIONS (APARTMENT/UNIT)</h2>
            <p>[Structure &amp; walls, openings, flooring, kitchen, toilets, electrification and painting specifications to be annexed.]</p>
            <h2>SCHEDULE E &mdash; SPECIFICATIONS (PROJECT)</h2>
            <p>[Project-level amenities and facilities to be annexed.]</p>

            <div class="footer center" style="margin-top:30px; font-size:9px; color:#888;">
              Generated by Safar &bull; This is an unsigned draft Agreement for Sale based on the RERA model form, for review only; it does not constitute a legally executed agreement and should be finalised with legal advice before registration.
            </div>
            </body></html>
            """;
}
