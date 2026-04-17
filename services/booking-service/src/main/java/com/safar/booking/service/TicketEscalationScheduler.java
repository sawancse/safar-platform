package com.safar.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.entity.TenancySettlement;
import com.safar.booking.entity.TicketComment;
import com.safar.booking.entity.enums.MaintenanceStatus;
import com.safar.booking.entity.enums.SettlementStatus;
import com.safar.booking.repository.MaintenanceRequestRepository;
import com.safar.booking.repository.TenancySettlementRepository;
import com.safar.booking.repository.TicketCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEscalationScheduler {

    private final MaintenanceRequestRepository requestRepository;
    private final TicketCommentRepository commentRepository;
    private final TenancySettlementRepository settlementRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /** Kafka producer uses StringSerializer — JSON-stringify before send. */
    private void sendEvent(String topic, String key, Object payload, UUID entityId) {
        try {
            kafkaTemplate.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            log.warn("Kafka {} payload serialization failed for {}: {}", topic, entityId, e.getMessage());
            kafkaTemplate.send(topic, key, "{\"id\":\"" + entityId + "\"}");
        }
    }

    private static final List<MaintenanceStatus> ACTIVE_STATUSES = List.of(
            MaintenanceStatus.OPEN, MaintenanceStatus.ASSIGNED,
            MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.REOPENED
    );

    /**
     * Check for SLA breaches every 15 minutes.
     * Auto-escalates: L1 → L2 → L3.
     */
    @Scheduled(fixedRate = 900000) // 15 minutes
    @Transactional
    public void checkSlaBreaches() {
        List<MaintenanceRequest> breached = requestRepository
                .findBySlaBreachedFalseAndStatusInAndSlaDeadlineAtBefore(
                        ACTIVE_STATUSES, OffsetDateTime.now());

        for (MaintenanceRequest ticket : breached) {
            ticket.setSlaBreached(true);

            // Auto-escalate
            int currentLevel = ticket.getEscalationLevel();
            if (currentLevel < 3) {
                ticket.setEscalationLevel(currentLevel + 1);
                ticket.setEscalatedAt(OffsetDateTime.now());

                String levelLabel = switch (ticket.getEscalationLevel()) {
                    case 2 -> "Area Manager (L2)";
                    case 3 -> "Admin/Central Ops (L3)";
                    default -> "L" + ticket.getEscalationLevel();
                };

                addSystemComment(ticket,
                        "SLA breached. Auto-escalated to " + levelLabel + ".");

                sendEvent("ticket.escalated", ticket.getId().toString(), ticket, ticket.getId());
                log.warn("Ticket {} SLA breached — escalated to L{}",
                        ticket.getRequestNumber(), ticket.getEscalationLevel());
            } else {
                addSystemComment(ticket,
                        "SLA breached. Already at highest escalation level (L3).");
                sendEvent("ticket.sla.breached", ticket.getId().toString(), ticket, ticket.getId());
            }

            requestRepository.save(ticket);
        }

        if (!breached.isEmpty()) {
            log.info("SLA breach check: {} tickets escalated", breached.size());
        }
    }

    /**
     * Check for overdue refund deadlines daily at 8 AM.
     */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void checkRefundDeadlines() {
        List<SettlementStatus> nonTerminalStatuses = List.of(
                SettlementStatus.INITIATED, SettlementStatus.INSPECTION_DONE,
                SettlementStatus.DEDUCTIONS_CALCULATED, SettlementStatus.APPROVED,
                SettlementStatus.DISPUTED, SettlementStatus.ADMIN_RESOLVED
        );

        List<TenancySettlement> overdue = settlementRepository
                .findByIsOverdueFalseAndRefundDeadlineDateBeforeAndStatusIn(
                        LocalDate.now(), nonTerminalStatuses);

        for (TenancySettlement settlement : overdue) {
            settlement.setOverdue(true);
            settlementRepository.save(settlement);

            sendEvent("settlement.refund.overdue", settlement.getId().toString(), settlement, settlement.getId());
            log.warn("Settlement {} is overdue — deadline was {}",
                    settlement.getSettlementRef(), settlement.getRefundDeadlineDate());
        }

        if (!overdue.isEmpty()) {
            log.info("Refund deadline check: {} settlements marked overdue", overdue.size());
        }
    }

    private void addSystemComment(MaintenanceRequest request, String text) {
        TicketComment comment = TicketComment.builder()
                .request(request)
                .authorId(UUID.fromString("00000000-0000-0000-0000-000000000000"))
                .authorRole("SYSTEM")
                .commentText(text)
                .systemNote(true)
                .build();
        commentRepository.save(comment);
    }
}
