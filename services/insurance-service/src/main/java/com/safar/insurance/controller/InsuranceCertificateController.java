package com.safar.insurance.controller;

import com.safar.insurance.entity.InsurancePolicy;
import com.safar.insurance.repository.InsurancePolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Public, policyRef-gated certificate of insurance. The certificate email links here
 * ({@code GET /api/v1/insurance/certificate/{policyRef}}), so the "Certificate emailed
 * to you" promise resolves to a real, viewable document.
 *
 * When a real insurer/aggregator policy carries the partner's own hosted certificate PDF,
 * we 302-redirect to it. Otherwise (Sandbox underwriter, or partner without a hosted PDF)
 * we render a self-hosted HTML certificate from the persisted policy.
 */
@RestController
@RequestMapping("/api/v1/insurance/certificate")
@RequiredArgsConstructor
public class InsuranceCertificateController {

    private final InsurancePolicyRepository policyRepository;

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @GetMapping(value = "/{policyRef}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> certificate(@PathVariable String policyRef) {
        Optional<InsurancePolicy> opt = policyRepository.findByPolicyRef(policyRef);
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(page("Certificate not found",
                            "<p style='color:#b91c1c'>No policy found for reference <b>" + esc(policyRef) + "</b>.</p>"));
        }
        InsurancePolicy p = opt.get();

        // Real partner-hosted certificate PDF → redirect to it (skip our placeholder sandbox URLs).
        String url = p.getCertificateUrl();
        if (url != null && url.startsWith("http") && !url.contains("sandbox.bhramankaro.com")) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(url)).build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(render(p));
    }

    private String render(InsurancePolicy p) {
        long premium = p.getPremiumPaise() != null ? p.getPremiumPaise() : 0;
        long cover = p.getSumInsuredPaise() != null ? p.getSumInsuredPaise() : 0;
        boolean active = p.getStatus() != null && p.getStatus().name().equals("ISSUED");
        String body = """
            <div style="max-width:640px;margin:24px auto;background:#fff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;">
              <div style="background:linear-gradient(135deg,#1a1a2e,#16213e);padding:24px 28px;color:#fff;">
                <div style="font-family:Georgia,serif;font-size:26px;font-weight:bold;color:#FF5A5F;">BhramanKaro</div>
                <div style="font-size:12px;letter-spacing:2px;opacity:.8;text-transform:uppercase;margin-top:2px;">Certificate of Insurance</div>
              </div>
              <div style="padding:24px 28px;">
                <table style="width:100%%;border-collapse:collapse;font-size:14px;color:#111827;">
                  %s
                </table>
                <p style="margin-top:20px;font-size:12px;color:#6b7280;line-height:1.6;">
                  Status: <b style="color:%s">%s</b>. This certificate is issued by BhramanKaro's insurance
                  partner. Coverage, terms and exclusions are governed by the policy wording. For claims or
                  assistance contact support@bhramankaro.com.
                </p>
              </div>
            </div>
            """.formatted(
                rows(
                        "Policy reference", esc(p.getPolicyRef()),
                        "Insurer / Provider", esc(p.getProvider()),
                        "Policy number", esc(p.getExternalPolicyId() != null ? p.getExternalPolicyId() : "—"),
                        "Coverage", esc(label(p.getCoverageType().name())),
                        "Sum insured", "₹" + fmt(cover),
                        "Premium paid", "₹" + fmt(premium),
                        "Insured persons", String.valueOf(p.getInsuredCount()),
                        "Valid from", p.getTripStartDate() != null ? p.getTripStartDate().format(D) : "—",
                        "Valid until", p.getTripEndDate() != null ? p.getTripEndDate().format(D) : "—"
                ),
                active ? "#15803d" : "#b91c1c",
                active ? "ACTIVE" : esc(p.getStatus() != null ? p.getStatus().name() : "—")
        );
        return page("Certificate " + esc(p.getPolicyRef()), body);
    }

    private static String rows(String... kv) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i + 1 < kv.length; i += 2) {
            sb.append("<tr>")
              .append("<td style='padding:8px 0;color:#6b7280;width:42%;border-bottom:1px solid #f3f4f6;'>")
              .append(esc(kv[i])).append("</td>")
              .append("<td style='padding:8px 0;font-weight:600;border-bottom:1px solid #f3f4f6;'>")
              .append(kv[i + 1]).append("</td>")
              .append("</tr>");
        }
        return sb.toString();
    }

    private static String page(String title, String inner) {
        return "<!doctype html><html><head><meta charset='utf-8'/>"
                + "<meta name='viewport' content='width=device-width,initial-scale=1'/>"
                + "<title>" + esc(title) + "</title></head>"
                + "<body style='margin:0;background:#f3f4f6;'>" + inner + "</body></html>";
    }

    private static String label(String enumName) {
        String s = enumName.replace('_', ' ').toLowerCase();
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String fmt(long paise) {
        return String.format("%,d", paise / 100);
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
