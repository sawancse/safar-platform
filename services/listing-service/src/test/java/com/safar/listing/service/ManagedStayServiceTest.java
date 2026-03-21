package com.safar.listing.service;

import com.safar.listing.dto.ManagedEnrollRequest;
import com.safar.listing.entity.ManagedStayContract;
import com.safar.listing.repository.ManagedStayContractRepository;
import com.safar.listing.repository.ManagedStayExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ManagedStayServiceTest {

    @Mock
    ManagedStayContractRepository contractRepository;
    @Mock
    ManagedStayExpenseRepository expenseRepository;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    ManagedStayService managedStayService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    @Test
    void enrollListing_success_createsActiveContract() {
        when(contractRepository.findByListingIdAndStatus(listingId, "ACTIVE"))
                .thenReturn(Optional.empty());
        when(contractRepository.save(any())).thenAnswer(inv -> {
            ManagedStayContract c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        ManagedEnrollRequest req = new ManagedEnrollRequest(
                20, LocalDate.of(2026, 4, 1), true, true, true);

        ManagedStayContract result = managedStayService.enrollListing(hostId, listingId, req);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getListingId()).isEqualTo(listingId);
        assertThat(result.getHostId()).isEqualTo(hostId);
        assertThat(result.getManagementFeePct()).isEqualTo(20);
        verify(kafkaTemplate).send(eq("managed.listing.enrolled"), any(), any());
    }

    @Test
    void enrollListing_duplicateActive_throwsConflict() {
        ManagedStayContract existing = ManagedStayContract.builder()
                .id(UUID.randomUUID())
                .listingId(listingId)
                .hostId(hostId)
                .status("ACTIVE")
                .build();
        when(contractRepository.findByListingIdAndStatus(listingId, "ACTIVE"))
                .thenReturn(Optional.of(existing));

        ManagedEnrollRequest req = new ManagedEnrollRequest(
                18, LocalDate.of(2026, 4, 1), true, true, true);

        assertThatThrownBy(() -> managedStayService.enrollListing(hostId, listingId, req))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already has an active managed-stay contract");
    }

    @Test
    void terminateContract_success_changesStatusToTerminated() {
        UUID contractId = UUID.randomUUID();
        ManagedStayContract contract = ManagedStayContract.builder()
                .id(contractId)
                .listingId(listingId)
                .hostId(hostId)
                .status("ACTIVE")
                .build();
        when(contractRepository.findById(contractId)).thenReturn(Optional.of(contract));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ManagedStayContract result = managedStayService.terminateContract(hostId, contractId);

        assertThat(result.getStatus()).isEqualTo("TERMINATED");
    }
}
