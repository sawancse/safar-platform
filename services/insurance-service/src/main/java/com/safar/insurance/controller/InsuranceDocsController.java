package com.safar.insurance.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Self-hosted insurance policy-wording document (the old sandbox.bhramankaro.com host
 * never existed → dead "Policy wording" links). Public, read-only; for a live insurer the
 * adapter returns the insurer's own wording URL instead.
 */
@RestController
@RequestMapping("/api/v1/insurance")
public class InsuranceDocsController {

    @GetMapping(value = "/policy-wording", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> policyWording(@RequestParam(required = false) String type) {
        String label = label(type);
        String html = """
            <!doctype html><html><head><meta charset='utf-8'/>
            <meta name='viewport' content='width=device-width,initial-scale=1'/>
            <title>%s — Policy Wording</title></head>
            <body style='margin:0;background:#f3f4f6;font-family:-apple-system,Segoe UI,Roboto,Arial,sans-serif;color:#111827;'>
              <div style='max-width:760px;margin:24px auto;background:#fff;border:1px solid #e5e7eb;border-radius:14px;overflow:hidden;'>
                <div style='background:linear-gradient(135deg,#1a1a2e,#16213e);padding:24px 30px;color:#fff;'>
                  <div style='font-family:Georgia,serif;font-size:24px;font-weight:bold;color:#FF5A5F;'>BhramanKaro</div>
                  <div style='font-size:12px;letter-spacing:2px;opacity:.8;text-transform:uppercase;margin-top:2px;'>%s — Policy Wording</div>
                </div>
                <div style='padding:26px 30px;line-height:1.7;font-size:14px;'>
                  <p style='background:#fff7ed;border:1px solid #fed7aa;color:#9a3412;padding:10px 12px;border-radius:8px;font-size:12px;'>
                    Specimen wording for illustration. The binding terms are those of the underwriting insurer's
                    policy document issued with your certificate.</p>
                  <h2 style='font-size:16px;'>1. Cover</h2>
                  <p>This policy provides %s cover as described in your certificate of insurance, subject to the sum
                     insured, sub-limits and the terms below.</p>
                  <h2 style='font-size:16px;'>2. Key exclusions</h2>
                  <ul>
                    <li>Pre-existing conditions during the waiting period (where applicable)</li>
                    <li>Claims arising from non-disclosure or misrepresentation</li>
                    <li>War, nuclear risks, and intentional self-inflicted loss</li>
                    <li>Anything expressly excluded in your certificate schedule</li>
                  </ul>
                  <h2 style='font-size:16px;'>3. Free-look period</h2>
                  <p>You may cancel within 15 days of issuance for a refund of premium less proportionate risk
                     and expenses, provided no claim has been made.</p>
                  <h2 style='font-size:16px;'>4. Claims</h2>
                  <p>Intimate claims via BhramanKaro Claims Assistance with your policy reference. Cashless (where
                     available) is coordinated with the network; otherwise reimbursement applies on document submission.</p>
                  <h2 style='font-size:16px;'>5. Grievances</h2>
                  <p>Contact support@bhramankaro.com. Unresolved grievances may be escalated to the insurer's
                     grievance officer and the Insurance Ombudsman.</p>
                  <p style='color:#6b7280;font-size:12px;margin-top:24px;'>BhramanKaro is an insurance distributor; insurance is underwritten by IRDAI-registered partners.</p>
                </div>
              </div>
            </body></html>
            """.formatted(esc(label), esc(label), esc(label.toLowerCase()));
        return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(html);
    }

    private static String label(String type) {
        if (type == null || type.isBlank()) return "Insurance";
        return switch (type) {
            case "INTERNATIONAL_TRAVEL" -> "International Travel Insurance";
            case "DOMESTIC_TRAVEL" -> "Domestic Travel Insurance";
            case "STUDENT_TRAVEL" -> "Student Travel Insurance";
            case "STAY_PROTECTION" -> "Stay Protection";
            case "TENANT_CONTENTS" -> "Renter / Tenant Cover";
            case "HEALTH" -> "Health Insurance";
            case "LIFE_TERM" -> "Term Life Insurance";
            case "MOTOR" -> "Motor Insurance";
            case "HOME" -> "Home Insurance";
            case "PERSONAL_ACCIDENT" -> "Personal Accident Cover";
            default -> {
                String s = type.replace('_', ' ').toLowerCase();
                yield Character.toUpperCase(s.charAt(0)) + s.substring(1);
            }
        };
    }

    private static String esc(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
