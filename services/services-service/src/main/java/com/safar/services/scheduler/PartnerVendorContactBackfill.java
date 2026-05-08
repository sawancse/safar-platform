package com.safar.services.scheduler;

import com.safar.services.entity.PartnerVendor;
import com.safar.services.repository.PartnerVendorRepository;
import com.safar.services.service.UserClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * One-shot startup pass: any self-service PartnerVendor row with a linked
 * user_id but blank phone/email gets repaired from user-service. Repairs
 * stubs created before the contact-fill landed in ensurePartnerVendorStub.
 * Once every row has contact details, this loop is a no-op.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PartnerVendorContactBackfill {

    private final PartnerVendorRepository partnerVendorRepo;
    private final UserClient userClient;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void backfill() {
        List<PartnerVendor> stale = partnerVendorRepo.findMissingContactWithUser();
        if (stale.isEmpty()) return;
        int repaired = 0;
        for (PartnerVendor v : stale) {
            UserClient.UserInfo profile = userClient.getUser(v.getUserId());
            if (profile == null) continue;
            boolean changed = false;
            if ((v.getPhone() == null || v.getPhone().isBlank()) && profile.hasPhone()) {
                v.setPhone(profile.phone());
                changed = true;
            }
            if ((v.getEmail() == null || v.getEmail().isBlank()) && profile.hasEmail()) {
                v.setEmail(profile.email());
                changed = true;
            }
            if (changed) repaired++;
        }
        if (repaired > 0) {
            partnerVendorRepo.saveAll(stale);
            log.info("Backfilled phone/email on {} PartnerVendor rows from user-service", repaired);
        }
    }
}
