package com.safar.listing.service;

import com.safar.listing.dto.GuaranteeRequest;
import com.safar.listing.entity.GuaranteedIncomeContract;
import com.safar.listing.repository.GuaranteedIncomeContractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuaranteedIncomeServiceTest {

    @Mock
    GuaranteedIncomeContractRepository contractRepository;
    @Mock
    KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    GuaranteedIncomeService guaranteedIncomeService;

    private final UUID hostId = UUID.randomUUID();
    private final UUID listingId = UUID.randomUUID();

    @Test
    void createContract_guaranteeCappedAt50k() {
        when(contractRepository.save(any())).thenAnswer(inv -> {
            GuaranteedIncomeContract c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        GuaranteeRequest req = new GuaranteeRequest(
                10_000_000L, LocalDate.of(2026, 4, 1), 6);

        GuaranteedIncomeContract result = guaranteedIncomeService.createContract(hostId, listingId, req);

        assertThat(result.getMonthlyGuaranteePaise()).isEqualTo(5_000_000L);
    }

    @Test
    void createContract_durationCappedAt12Months() {
        when(contractRepository.save(any())).thenAnswer(inv -> {
            GuaranteedIncomeContract c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        GuaranteeRequest req = new GuaranteeRequest(
                3_000_000L, LocalDate.of(2026, 4, 1), 24);

        GuaranteedIncomeContract result = guaranteedIncomeService.createContract(hostId, listingId, req);

        assertThat(result.getContractEnd()).isEqualTo(LocalDate.of(2027, 4, 1));
    }

    @Test
    void createContract_success_createsActiveContract() {
        when(contractRepository.save(any())).thenAnswer(inv -> {
            GuaranteedIncomeContract c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        GuaranteeRequest req = new GuaranteeRequest(
                2_000_000L, LocalDate.of(2026, 4, 1), 6);

        GuaranteedIncomeContract result = guaranteedIncomeService.createContract(hostId, listingId, req);

        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getListingId()).isEqualTo(listingId);
        assertThat(result.getHostId()).isEqualTo(hostId);
        assertThat(result.getMonthlyGuaranteePaise()).isEqualTo(2_000_000L);
        assertThat(result.getContractEnd()).isEqualTo(LocalDate.of(2026, 10, 1));
        verify(kafkaTemplate).send(eq("guarantee.contract.created"), any(), any());
    }
}
