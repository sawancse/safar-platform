package com.safar.listing.service;

import com.safar.listing.entity.ScoutLead;
import com.safar.listing.entity.enums.ScoutLeadStatus;
import com.safar.listing.repository.ScoutLeadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScoutLeadServiceTest {

    @Mock
    ScoutLeadRepository scoutLeadRepository;

    @InjectMocks
    ScoutLeadService scoutLeadService;

    @Test
    void createLead_savesWithPendingStatus() {
        when(scoutLeadRepository.save(any(ScoutLead.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ScoutLead lead = scoutLeadService.createLead(
                "123 MG Road, Mumbai", "Mumbai", 350000L);

        assertThat(lead.getAddress()).isEqualTo("123 MG Road, Mumbai");
        assertThat(lead.getCity()).isEqualTo("Mumbai");
        assertThat(lead.getEstimatedIncomePaise()).isEqualTo(350000L);
        assertThat(lead.getStatus()).isEqualTo(ScoutLeadStatus.PENDING);
        verify(scoutLeadRepository).save(any(ScoutLead.class));
    }

    @Test
    void updateStatus_changesStatusToContacted() {
        UUID leadId = UUID.randomUUID();
        ScoutLead existing = ScoutLead.builder()
                .id(leadId)
                .address("456 Park Street, Delhi")
                .city("Delhi")
                .estimatedIncomePaise(300000L)
                .status(ScoutLeadStatus.PENDING)
                .build();

        when(scoutLeadRepository.findById(leadId)).thenReturn(Optional.of(existing));
        when(scoutLeadRepository.save(any(ScoutLead.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ScoutLead updated = scoutLeadService.updateStatus(leadId, ScoutLeadStatus.CONTACTED);

        assertThat(updated.getStatus()).isEqualTo(ScoutLeadStatus.CONTACTED);
        verify(scoutLeadRepository).save(any(ScoutLead.class));
    }
}
