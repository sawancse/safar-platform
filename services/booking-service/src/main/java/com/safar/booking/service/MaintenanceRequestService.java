package com.safar.booking.service;

import com.safar.booking.dto.CreateMaintenanceRequestDto;
import com.safar.booking.dto.UpdateMaintenanceRequestDto;
import com.safar.booking.entity.MaintenanceRequest;
import com.safar.booking.entity.enums.MaintenanceCategory;
import com.safar.booking.entity.enums.MaintenancePriority;
import com.safar.booking.entity.enums.MaintenanceStatus;
import com.safar.booking.repository.MaintenanceRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceRequestService {

    private final MaintenanceRequestRepository requestRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static long requestCounter = 1000;

    @Transactional
    public MaintenanceRequest createRequest(UUID tenancyId, CreateMaintenanceRequestDto req) {
        MaintenanceRequest request = MaintenanceRequest.builder()
                .tenancyId(tenancyId)
                .requestNumber("MR-" + String.format("%04d", ++requestCounter))
                .category(MaintenanceCategory.valueOf(req.category()))
                .title(req.title())
                .description(req.description())
                .photoUrls(req.photoUrls())
                .priority(req.priority() != null ? MaintenancePriority.valueOf(req.priority()) : MaintenancePriority.MEDIUM)
                .status(MaintenanceStatus.OPEN)
                .build();

        MaintenanceRequest saved = requestRepository.save(request);
        kafkaTemplate.send("maintenance.request.created", saved.getId().toString(), saved);
        log.info("Maintenance request {} created for tenancy {}: {}", saved.getRequestNumber(), tenancyId, req.title());
        return saved;
    }

    @Transactional
    public MaintenanceRequest updateRequest(UUID requestId, UpdateMaintenanceRequestDto req) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found: " + requestId));

        if (req.status() != null) {
            MaintenanceStatus newStatus = MaintenanceStatus.valueOf(req.status());
            request.setStatus(newStatus);
            if (newStatus == MaintenanceStatus.RESOLVED) {
                request.setResolvedAt(OffsetDateTime.now());
                kafkaTemplate.send("maintenance.request.resolved", request.getId().toString(), request);
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
    public MaintenanceRequest rateResolution(UUID requestId, int rating, String feedback) {
        MaintenanceRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found: " + requestId));

        if (request.getStatus() != MaintenanceStatus.RESOLVED && request.getStatus() != MaintenanceStatus.CLOSED) {
            throw new RuntimeException("Can only rate resolved/closed requests");
        }

        request.setTenantRating(rating);
        request.setTenantFeedback(feedback);
        request.setStatus(MaintenanceStatus.CLOSED);
        return requestRepository.save(request);
    }

    public Page<MaintenanceRequest> getRequests(UUID tenancyId, String status, Pageable pageable) {
        if (status != null && !status.isEmpty()) {
            return requestRepository.findByTenancyIdAndStatusOrderByCreatedAtDesc(
                    tenancyId, MaintenanceStatus.valueOf(status), pageable);
        }
        return requestRepository.findByTenancyIdOrderByCreatedAtDesc(tenancyId, pageable);
    }

    public MaintenanceRequest getRequest(UUID requestId) {
        return requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Maintenance request not found: " + requestId));
    }

    public long countOpen(UUID tenancyId) {
        return requestRepository.countByTenancyIdAndStatus(tenancyId, MaintenanceStatus.OPEN);
    }
}
