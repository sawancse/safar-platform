package com.safar.payment.service;

import com.safar.payment.entity.Payout;
import com.safar.payment.entity.SettlementLine;
import com.safar.payment.entity.SettlementPlan;
import com.safar.payment.entity.enums.PayoutStatus;
import com.safar.payment.entity.enums.RecipientType;
import com.safar.payment.entity.enums.SettlementStatus;
import com.safar.payment.repository.PayoutItemRepository;
import com.safar.payment.repository.PayoutRepository;
import com.safar.payment.repository.SettlementLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PayoutServiceTest {

    @Mock PayoutRepository payoutRepo;
    @Mock SettlementLineRepository lineRepo;
    @Mock PayoutItemRepository itemRepo;
    @Mock PayoutEligibility eligibility;
    @Mock RazorpayXClient razorpayX;
    @Mock RestTemplate restTemplate;
    @InjectMocks PayoutService service;

    UUID lineId = UUID.randomUUID();
    UUID hostId = UUID.randomUUID();
    SettlementLine line;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "userServiceUrl", "http://user");
        ReflectionTestUtils.setField(service, "maxRetries", 3);
        SettlementPlan plan = SettlementPlan.builder().bookingId(UUID.randomUUID()).build();
        line = SettlementLine.builder().id(lineId).plan(plan).recipientType(RecipientType.HOST)
                .recipientId(hostId).amountPaise(246000L).status(SettlementStatus.COMPLETED).build();
        lenient().when(lineRepo.findById(lineId)).thenReturn(Optional.of(line));
        lenient().when(payoutRepo.save(any(Payout.class))).thenAnswer(i -> {
            Payout p = i.getArgument(0);
            if (p.getId() == null) p.setId(UUID.randomUUID());
            return p;
        });
        lenient().when(payoutRepo.saveAndFlush(any(Payout.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void existingProcessingPayoutIsNeverSentTwice() throws Exception {
        Payout existing = Payout.builder().id(UUID.randomUUID()).hostId(hostId).amountPaise(246000L)
                .netAmountPaise(246000L).method("BANK").status(PayoutStatus.PROCESSING).build();
        when(payoutRepo.findBySettlementLineId(lineId)).thenReturn(Optional.of(existing));

        Payout out = service.executeForSettlementLine(lineId, true);

        assertSame(existing, out);
        verifyNoInteractions(razorpayX);
    }

    @Test
    void unverifiedKycFailsClosedWithoutCallingRazorpay() throws Exception {
        when(payoutRepo.findBySettlementLineId(lineId)).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(Map.class), any(UUID.class)))
                .thenReturn(Map.of("kycVerified", false, "reason", "KYC status SUBMITTED"));

        Payout out = service.executeForSettlementLine(lineId, true);

        assertEquals(PayoutStatus.FAILED, out.getStatus());
        assertTrue(out.getFailureReason().contains("KYC"));
        verifyNoInteractions(razorpayX);
    }

    @Test
    void successfulSendStoresRazorpayIdAndGoesProcessing() throws Exception {
        when(payoutRepo.findBySettlementLineId(lineId)).thenReturn(Optional.empty());
        when(restTemplate.getForObject(anyString(), eq(Map.class), any(UUID.class))).thenReturn(Map.of(
                "kycVerified", true, "name", "Host", "accountNumber", "1234567890", "ifsc", "HDFC0000001", "email", "h@x.com"));
        when(razorpayX.createContact(any(), any(), any())).thenReturn("cont_1");
        when(razorpayX.createBankFundAccount("cont_1", "Host", "HDFC0000001", "1234567890")).thenReturn("fa_1");
        when(razorpayX.createPayout(eq("fa_1"), eq(false), eq(246000L), any(), any(), any()))
                .thenReturn(new RazorpayXClient.PayoutResult("pout_1", "queued", "fa_1", null));

        Payout out = service.executeForSettlementLine(lineId, true);

        assertEquals(PayoutStatus.PROCESSING, out.getStatus());
        assertEquals("pout_1", out.getRazorpayPayoutId());
        assertNotNull(out.getIdempotencyKey());
    }

    @Test
    void unsettledLineIsRejected() {
        line.setStatus(SettlementStatus.PENDING);
        assertThrows(IllegalStateException.class, () -> service.executeForSettlementLine(lineId, true));
    }

    @Test
    void retryStopsAtLimit() {
        UUID id = UUID.randomUUID();
        Payout p = Payout.builder().id(id).hostId(hostId).status(PayoutStatus.FAILED).retryCount(3).build();
        when(payoutRepo.findById(id)).thenReturn(Optional.of(p));
        assertThrows(IllegalStateException.class, () -> service.retry(id));
    }

    @Test
    void notYetEligibleLineIsRejectedUnlessForced() {
        line.setPayoutEligibleAt(OffsetDateTime.now().plusDays(1));
        when(payoutRepo.findBySettlementLineId(lineId)).thenReturn(Optional.empty());
        when(itemRepo.findBySettlementLineIdIn(any())).thenReturn(List.of());
        assertThrows(IllegalStateException.class, () -> service.executeForSettlementLine(lineId, false));
    }

    @Test
    void unknownCheckoutIsNotEligible() {
        line.setPayoutEligibleAt(null);
        when(payoutRepo.findBySettlementLineId(lineId)).thenReturn(Optional.empty());
        when(itemRepo.findBySettlementLineIdIn(any())).thenReturn(List.of());
        when(eligibility.eligibleAt(any())).thenReturn(Optional.empty());
        assertThrows(IllegalStateException.class, () -> service.executeForSettlementLine(lineId, false));
    }

    @Test
    void batchCombinesEligibleLinesIntoOnePayoutAndSkipsFutureOnes() throws Exception {
        SettlementPlan p2 = SettlementPlan.builder().bookingId(UUID.randomUUID()).build();
        SettlementPlan p3 = SettlementPlan.builder().bookingId(UUID.randomUUID()).build();
        line.setPayoutEligibleAt(OffsetDateTime.now().minusDays(1));
        SettlementLine l2 = SettlementLine.builder().id(UUID.randomUUID()).plan(p2).recipientType(RecipientType.HOST)
                .recipientId(hostId).amountPaise(100000L).status(SettlementStatus.COMPLETED)
                .payoutEligibleAt(OffsetDateTime.now().minusHours(1)).build();
        SettlementLine future = SettlementLine.builder().id(UUID.randomUUID()).plan(p3).recipientType(RecipientType.HOST)
                .recipientId(hostId).amountPaise(999L).status(SettlementStatus.COMPLETED)
                .payoutEligibleAt(OffsetDateTime.now().plusDays(1)).build();
        when(lineRepo.findUnpaidHostLines()).thenReturn(List.of(line, l2, future));
        when(restTemplate.getForObject(anyString(), eq(Map.class), any(UUID.class))).thenReturn(Map.of(
                "kycVerified", true, "name", "Host", "accountNumber", "1234567890", "ifsc", "HDFC0000001"));
        when(razorpayX.createContact(any(), any(), any())).thenReturn("cont_1");
        when(razorpayX.createBankFundAccount(any(), any(), any(), any())).thenReturn("fa_1");
        when(razorpayX.createPayout(eq("fa_1"), eq(false), eq(346000L), any(), any(), any()))
                .thenReturn(new RazorpayXClient.PayoutResult("pout_9", "queued", "fa_1", null));

        Payout out = service.executeBatchForHost(hostId, false).orElseThrow();

        assertEquals(346000L, out.getNetAmountPaise());
        assertEquals(PayoutStatus.PROCESSING, out.getStatus());
        assertNull(out.getBookingId());
        verify(itemRepo, times(2)).save(any());
    }

    @Test
    void batchWithNothingEligibleSendsNothing() throws Exception {
        line.setPayoutEligibleAt(OffsetDateTime.now().plusDays(1));
        when(lineRepo.findUnpaidHostLines()).thenReturn(List.of(line));
        assertTrue(service.executeBatchForHost(hostId, false).isEmpty());
        verifyNoInteractions(razorpayX);
    }
}
