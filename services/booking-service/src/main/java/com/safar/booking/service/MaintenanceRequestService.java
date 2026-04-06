package com.safar.booking.service;

import com.safar.booking.dto.*;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.entity.PgTenancy;
import com.safar.booking.entity.TicketComment;
import com.safar.booking.entity.enums.MaintenanceCategory;
import com.safar.booking.entity.enums.MaintenancePriority;
import com.safar.booking.entity.enums.MaintenanceStatus;
import com.safar.booking.repository.MaintenanceRequestRepository;
import com.safar.booking.repository.PgTenancyRepository;
import com.safar.booking.repository.TicketCommentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository requestRepository;
    private final TicketCommentRepository commentRepository;
    private final PgTenancyRepository tenancyRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static long requestCounter = 1000;

    // SLA hours per priority
    private static final Map<MaintenancePriority, Integer> SLA_HOURS = Map.of(
            MaintenancePriority.URGENT, 4,
            MaintenancePriority.HIGH, 12,
            MaintenancePriority.MEDIUM, 24,
            MaintenancePriority.LOW, 48
    );

    @Transactional
    public MaintenanceRequest createRequest(UUID tenancyId, CreateMaintenanceRequestDto req) {
        // Lookup listing from tenancy
        UUID listingId = tenancyRepository.findById(tenancyId)
                .map(PgTenancy::getListingId)
                .orElse(null);

        MaintenancePriority priority = req.priority() != null
                ? MaintenancePriority.valueOf(req.priority())
                : MaintenancePriority.MEDIUM;

        OffsetDateTime slaDeadline = OffsetDateTime.now().plusHours(SLA_HOURS.get(priority));

        MaintenanceRequest request = MaintenanceRequest.builder()
                .tenancyId(tenancyId)
                .listingId(listingId)
                .requestNumber("MR-" + String.format("%04d", ++requestCounter))
                .category(MaintenanceCategory.valueOf(req.category()))
                .title(req.title())
                .description(req.description())
                .photoUrls(req.photoUrls())
                .priority(priority)
                .status(MaintenanceStatus.OPEN)
                .slaDeadlineAt(slaDeadline)
                .escalationLevel(1)
                .build();

        MaintenanceRequest saved = requestRepository.save(request);

        // Add system comment
        addSystemComment(saved, "Ticket created. SLA deadline: " + SLA_HOURS.get(priority) + " hours.");

        kafkaTemplate.send("maintenance.request.created", saved.getId().toString(), saved);
        log.info("Ticket {} created for tenancy {}: {}", saved.getRequestNumber(), tenancyId, req.title());
        return saved;
    }

    @Transactional
    public MaintenanceRequest updateRequest(UUID requestId, UpdateMaintenanceRequestDto req) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found: " + requestId));

        if (req.status() != null) {
            MaintenanceStatus newStatus = MaintenanceStatus.valueOf(req.status());
            request.setStatus(newStatus);

            if (newStatus == MaintenanceStatus.ASSIGNED && request.getAssignedAt() == null) {
                request.setAssignedAt(OffsetDateTime.now());
                addSystemComment(request, "Ticket assigned to: " + (req.assignedTo() != null ? req.assignedTo() : request.getAssignedTo()));
                kafkaTemplate.send("ticket.assigned", request.getId().toString(), request);
            }
            if (newStatus == MaintenanceStatus.RESOLVED) {
                request.setResolvedAt(OffsetDateTime.now());
                addSystemComment(request, "Ticket resolved.");
                kafkaTemplate.send("maintenance.request.resolved", request.getId().toString(), request);
            }
            if (newStatus == MaintenanceStatus.IN_PROGRESS) {
                addSystemComment(request, "Ticket is now in progress.");
            }
            if (newStatus == MaintenanceStatus.REJECTED) {
                addSystemComment(request, "Ticket rejected. Reason: " + (req.resolutionNotes() != null ? req.resolutionNotes() : "N/A"));
            }
        }
        if (req.assignedTo() != null) {
            request.setAssignedTo(req.assignedTo());
            if (request.getAssignedAt() == null) {
                request.setAssignedAt(OffsetDateTime.now());
            }
        }
        if (req.resolutionNotes() != null) {
            request.setResolutionNotes(req.resolutionNotes());
        }

        return requestRepository.save(request);
    }

    @Transactional
    public MaintenanceRequest reopenTicket(UUID requestId, String reason) {
        MaintenanceRequest request = getRequest(requestId);

        if (request.getStatus() != MaintenanceStatus.RESOLVED) {
            throw new RuntimeException("Only RESOLVED tickets can be reopened");
        }

        // Only allow reopen within 48 hours of resolution
        if (request.getResolvedAt() != null &&
                Duration.between(request.getResolvedAt(), OffsetDateTime.now()).toHours() > 48) {
            throw new RuntimeException("Cannot reopen ticket after 48 hours of resolution");
        }

        request.setStatus(MaintenanceStatus.REOPENED);
        request.setReopenedAt(OffsetDateTime.now());
        request.setReopenCount(request.getReopenCount() + 1);
        request.setResolvedAt(null);

        // Recalculate SLA from now
        int slaHours = SLA_HOURS.get(request.getPriority());
        request.setSlaDeadlineAt(OffsetDateTime.now().plusHours(slaHours));
        request.setSlaBreached(false);

        MaintenanceRequest saved = requestRepository.save(request);
        addSystemComment(saved, "Ticket reopened by tenant. Reason: " + reason);
        kafkaTemplate.send("ticket.reopened", saved.getId().toString(), saved);
        log.info("Ticket {} reopened (count: {})", saved.getRequestNumber(), saved.getReopenCount());
        return saved;
    }

    @Transactional
    public MaintenanceRequest closeTicket(UUID requestId) {
        MaintenanceRequest request = getRequest(requestId);

        if (request.getStatus() != MaintenanceStatus.RESOLVED) {
            throw new RuntimeException("Only RESOLVED tickets can be closed by tenant");
        }

        request.setStatus(MaintenanceStatus.CLOSED);
        request.setClosedAt(OffsetDateTime.now());
        addSystemComment(request, "Ticket closed by tenant.");
        return requestRepository.save(request);
    }

    @Transactional
    public MaintenanceRequest rateResolution(UUID requestId, int rating, String feedback) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found: " + requestId));

        if (request.getStatus() != MaintenanceStatus.RESOLVED && request.getStatus() != MaintenanceStatus.CLOSED) {
            throw new RuntimeException("Can only rate resolved/closed requests");
        }

        request.setTenantRating(rating);
        request.setTenantFeedback(feedback);
        request.setStatus(MaintenanceStatus.CLOSED);
        request.setClosedAt(OffsetDateTime.now());
        return requestRepository.save(request);
    }

    // ── Comments ──────────────────────────────────────────────

    @Transactional
    public TicketCommentResponse addComment(UUID requestId, UUID authorId, String authorRole,
                                            TicketCommentDto dto) {
        MaintenanceRequest request = getRequest(requestId);
        TicketComment comment = TicketComment.builder()
                .request(request)
                .authorId(authorId)
                .authorRole(authorRole)
                .commentText(dto.commentText())
                .attachmentUrls(dto.attachmentUrls())
                .systemNote(false)
                .build();
        TicketComment saved = commentRepository.save(comment);
        return toCommentResponse(saved);
    }

    public List<TicketCommentResponse> getComments(UUID requestId) {
        return commentRepository.findByRequestIdOrderByCreatedAtAsc(requestId)
                .stream()
                .map(this::toCommentResponse)
                .toList();
    }

    // ── Queries ──────────────────────────────────────────────

    public Page<MaintenanceRequest> getRequests(UUID tenancyId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return requestRepository.findByTenancyIdAndStatusOrderByCreatedAtDesc(
                    tenancyId, MaintenanceStatus.valueOf(status), pageable);
        }
        return requestRepository.findByTenancyIdOrderByCreatedAtDesc(tenancyId, pageable);
    }

    public Page<MaintenanceRequest> getTicketsByListing(UUID listingId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return requestRepository.findByListingIdAndStatusOrderByCreatedAtDesc(
                    listingId, MaintenanceStatus.valueOf(status), pageable);
        }
        return requestRepository.findByListingIdOrderByCreatedAtDesc(listingId, pageable);
    }

    public Page<MaintenanceRequest> getAllTickets(List<MaintenanceStatus> statuses, Pageable pageable) {
        if (statuses != null && !statuses.isEmpty()) {
            return requestRepository.findByStatusInOrderByCreatedAtDesc(statuses, pageable);
        }
        return requestRepository.findAll(pageable);
    }

    public Page<MaintenanceRequest> getSlaBreachedTickets(Pageable pageable) {
        List<MaintenanceStatus> activeStatuses = List.of(
                MaintenanceStatus.OPEN, MaintenanceStatus.ASSIGNED,
                MaintenanceStatus.IN_PROGRESS, MaintenanceStatus.REOPENED);
        return requestRepository.findBySlaBreachedTrueAndStatusInOrderByCreatedAtDesc(activeStatuses, pageable);
    }

    public MaintenanceRequest getRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found: " + requestId));
    }

    public long countOpen(UUID tenancyId) {
        return requestRepository.countByTenancyIdAndStatus(tenancyId, MaintenanceStatus.OPEN);
    }

    // ── Stats ──────────────────────────────────────────────

    public TicketStatsResponse getTicketStats(UUID listingId) {
        long openCount = requestRepository.countByListingIdAndStatus(listingId, MaintenanceStatus.OPEN);
        long inProgressCount = requestRepository.countByListingIdAndStatus(listingId, MaintenanceStatus.IN_PROGRESS);
        long resolvedCount = requestRepository.countByListingIdAndStatus(listingId, MaintenanceStatus.RESOLVED);
        long closedCount = requestRepository.countByListingIdAndStatus(listingId, MaintenanceStatus.CLOSED);
        long slaBreachedCount = requestRepository.countSlaBreachedByListing(listingId);
        Double avgResolutionHours = requestRepository.avgResolutionHoursByListing(listingId);

        // SLA compliance = (resolved without breach) / total resolved
        long totalResolved = resolvedCount + closedCount;
        double slaCompliance = totalResolved > 0
                ? ((double) (totalResolved - slaBreachedCount) / totalResolved) * 100.0
                : 100.0;

        // Category breakdown
        Map<String, Long> categoryBreakdown = requestRepository.countByListingGroupedByCategory(listingId)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((MaintenanceCategory) row[0]).name(),
                        row -> (Long) row[1]
                ));

        return new TicketStatsResponse(
                openCount, inProgressCount, resolvedCount, closedCount,
                slaBreachedCount, avgResolutionHours, slaCompliance, categoryBreakdown
        );
    }

    // ── Detail response ──────────────────────────────────────

    public TicketDetailResponse toDetailResponse(MaintenanceRequest r) {
        List<TicketCommentResponse> comments = commentRepository
                .findByRequestIdOrderByCreatedAtAsc(r.getId())
                .stream()
                .map(this::toCommentResponse)
                .toList();

        return new TicketDetailResponse(
                r.getId(), r.getTenancyId(), r.getListingId(),
                r.getRequestNumber(), r.getCategory().name(),
                r.getTitle(), r.getDescription(), r.getPhotoUrls(),
                r.getPriority().name(), r.getStatus().name(),
                r.getAssignedTo(), r.getAssignedAt(),
                r.getResolvedAt(), r.getResolutionNotes(),
                r.getTenantRating(), r.getTenantFeedback(),
                r.getSlaDeadlineAt(), r.isSlaBreached(),
                r.getEscalationLevel(), r.getEscalatedAt(),
                r.getReopenedAt(), r.getReopenCount(),
                r.getClosedAt(), comments,
                r.getCreatedAt(), r.getUpdatedAt()
        );
    }

    // ── Internal helpers ──────────────────────────────────────

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

    private TicketCommentResponse toCommentResponse(TicketComment c) {
        return new TicketCommentResponse(
                c.getId(), c.getAuthorId(), c.getAuthorRole(),
                c.getCommentText(), c.getAttachmentUrls(),
                c.isSystemNote(), c.getCreatedAt()
        );
    }
}
