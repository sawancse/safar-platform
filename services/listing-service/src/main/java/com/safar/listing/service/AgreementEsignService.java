package com.safar.listing.service;

import com.safar.listing.entity.AgreementParty;
import com.safar.listing.entity.AgreementRequest;
import com.safar.listing.entity.enums.AgreementStatus;
import com.safar.listing.entity.enums.ESignStatus;
import com.safar.listing.esign.AgreementProviderResolver;
import com.safar.listing.esign.EsignModels;
import com.safar.listing.esign.EsignProvider;
import com.safar.listing.repository.AgreementPartyRepository;
import com.safar.listing.repository.AgreementRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Orchestrates government e-Stamp + Aadhaar eSign for sale/rental agreements
 * (AgreementRequest / AgreementParty). Flow: issueEstamp -> initiateEsign ->
 * per-party Aadhaar signing -> finalize (all signed) -> status SIGNED. Provider
 * is pluggable (Sandbox by default, Digio when configured).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AgreementEsignService {

    private final AgreementRequestRepository agreementRepo;
    private final AgreementPartyRepository partyRepo;
    private final AgreementPdfService pdfService;
    private final AgreementProviderResolver providers;
    private final org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    private AgreementRequest load(UUID id) {
        return agreementRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Agreement not found: " + id));
    }

    /** Purchase a government e-stamp and attach the certificate to the agreement. */
    @Transactional
    public AgreementRequest issueEstamp(UUID agreementId, UUID userId) {
        AgreementRequest a = load(agreementId);
        assertOwner(a, userId);
        if (a.getStampCertificateNumber() != null) return a;   // already stamped
        List<AgreementParty> parties = partyRepo.findByAgreementRequestId(agreementId);
        var req = new EsignModels.EStampRequest(
                a.getState(),
                a.getAgreementType() != null ? a.getAgreementType().name() : "AGREEMENT",
                a.getSaleConsiderationPaise() != null ? a.getSaleConsiderationPaise() : 0L,
                a.getStampDutyPaise() != null ? a.getStampDutyPaise() : 0L,
                partyName(parties, 0), partyName(parties, 1),
                "Agreement execution");
        var res = providers.estamp().issueStamp(req);
        a.setStampCertificateNumber(res.certificateNo());
        a.setEstampProvider(res.provider());
        a.setEstampPdfUrl(res.stampPdfUrl());
        a.setEstampIssuedAt(res.issuedAt() != null ? res.issuedAt() : OffsetDateTime.now());
        a.setStatus(AgreementStatus.STAMPED);
        log.info("Agreement {} e-stamped: cert {} via {}", agreementId, res.certificateNo(), res.provider());
        return agreementRepo.save(a);
    }

    /** Create the eSign envelope and hand back per-party Aadhaar signing links. */
    @Transactional
    public EsignModels.EsignEnvelope initiateEsign(UUID agreementId, UUID userId) {
        AgreementRequest a = load(agreementId);
        assertOwner(a, userId);
        List<AgreementParty> parties = partyRepo.findByAgreementRequestId(agreementId);
        if (parties.isEmpty()) throw new IllegalStateException("Add parties before sending for eSign");

        byte[] pdf = pdfService.generateDraftPdf(a, parties);
        List<EsignModels.EsignSigner> signers = parties.stream()
                .map(p -> new EsignModels.EsignSigner(p.getId().toString(), p.getFullName(), p.getEmail(), p.getPhone(), p.getFullName()))
                .collect(Collectors.toList());

        EsignProvider provider = providers.esign();
        var envelope = provider.createEnvelope(new EsignModels.EsignCreateRequest(
                agreementId, "Agreement-" + a.getId(), pdf, signers, "Agreement execution"));

        a.setEsignProvider(provider.name());
        a.setEsignDocumentId(envelope.documentId());
        a.setEsignStatus("INITIATED");
        a.setStatus(AgreementStatus.PENDING_SIGN);
        agreementRepo.save(a);

        Map<String, String> linkByParty = envelope.signerLinks().stream()
                .collect(Collectors.toMap(EsignModels.SignerLink::partyRef, EsignModels.SignerLink::signingUrl, (x, y) -> x));
        for (AgreementParty p : parties) {
            p.setESignRequestId(envelope.documentId());
            p.setESignStatus(ESignStatus.PENDING);
            String link = linkByParty.get(p.getId().toString());
            if (link != null) p.setEsignSigningUrl(link);
            partyRepo.save(p);
        }
        log.info("Agreement {} eSign initiated: doc {} via {}", agreementId, envelope.documentId(), provider.name());
        return envelope;
    }

    /** Sandbox shortcut: a signer "completes" Aadhaar eSign (called by the sandbox-sign endpoint). */
    @Transactional
    public void sandboxComplete(String documentId, String partyRef) {
        markPartySigned(documentId, partyRef, "XXXX-XXXX-0000");
    }

    /** Real provider webhook entry — verify, parse, mark signed, finalize when all done. */
    @Transactional
    public void handleWebhook(String payload, String signature) {
        EsignProvider provider = providers.esign();
        if (!provider.verifyWebhook(payload, signature)) {
            throw new SecurityException("Invalid eSign webhook signature");
        }
        var e = provider.parseWebhook(payload);
        if (e != null && e.signed() && e.documentId() != null) {
            markPartySigned(e.documentId(), e.partyRef(), e.vidMasked());
        }
    }

    private void markPartySigned(String documentId, String partyRef, String vidMasked) {
        AgreementRequest a = agreementRepo.findByEsignDocumentId(documentId)
                .orElseThrow(() -> new NoSuchElementException("No agreement for eSign doc " + documentId));
        List<AgreementParty> parties = partyRepo.findByAgreementRequestId(a.getId());
        for (AgreementParty p : parties) {
            if (partyRef == null || p.getId().toString().equals(partyRef)) {
                p.setESignStatus(ESignStatus.SIGNED);
                p.setSignedAt(OffsetDateTime.now());
                if (vidMasked != null) p.setEsignSignerVidMasked(vidMasked);
                partyRepo.save(p);
            }
        }
        boolean allSigned = parties.stream().allMatch(p -> p.getESignStatus() == ESignStatus.SIGNED);
        a.setEsignStatus(allSigned ? "SIGNED" : "PARTIALLY_SIGNED");
        if (allSigned) finalizeSigned(a, documentId);
        agreementRepo.save(a);
    }

    private void finalizeSigned(AgreementRequest a, String documentId) {
        // The signed PKCS#7 PDF is served on-demand via the proxy endpoint (downloadSignedPdf),
        // which streams from the provider; sandbox falls back to the draft.
        a.setSignedDocumentUrl("/api/v1/agreements/" + a.getId() + "/document/signed.pdf");
        a.setStatus(AgreementStatus.SIGNED);
        try {
            kafkaTemplate.send("agreement.signed", a.getId().toString(), a.getId().toString());
        } catch (Exception ex) {
            log.warn("Kafka agreement.signed emit failed for {}: {}", a.getId(), ex.getMessage());
        }
        log.info("Agreement {} fully signed via eSign doc {}", a.getId(), documentId);
    }

    /** Stream the signed PDF: from the provider for real eSign, draft fallback for sandbox/none. */
    public byte[] downloadSignedPdf(UUID agreementId) {
        AgreementRequest a = load(agreementId);
        if (a.getEsignDocumentId() != null) {
            try {
                byte[] b = providers.esign().downloadSignedPdf(a.getEsignDocumentId());
                if (b != null && b.length > 0) return b;
            } catch (Exception ex) {
                log.warn("Signed-PDF download failed for {}: {}", agreementId, ex.getMessage());
            }
        }
        return pdfService.generateDraftPdf(a, partyRepo.findByAgreementRequestId(agreementId));
    }

    public AgreementRequest getStatus(UUID agreementId) { return load(agreementId); }

    private static String partyName(List<AgreementParty> parties, int idx) {
        return parties.size() > idx ? parties.get(idx).getFullName() : null;
    }

    private static void assertOwner(AgreementRequest a, UUID userId) {
        if (userId != null && a.getUserId() != null && !a.getUserId().equals(userId)) {
            throw new SecurityException("Not your agreement");
        }
    }
}
