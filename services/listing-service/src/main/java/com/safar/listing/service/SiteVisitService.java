package com.safar.listing.service;

import com.safar.listing.dto.ScheduleVisitRequest;
import com.safar.listing.dto.SiteVisitResponse;
import com.safar.listing.entity.PropertyInquiry;
import com.safar.listing.entity.SaleProperty;
import com.safar.listing.entity.SiteVisit;
import com.safar.listing.entity.enums.InquiryStatus;
import com.safar.listing.entity.enums.VisitStatus;
import com.safar.listing.repository.PropertyInquiryRepository;
import com.safar.listing.repository.SalePropertyRepository;
import com.safar.listing.repository.SiteVisitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SiteVisitService {

    private final SiteVisitRepository visitRepository;
    private final SalePropertyRepository salePropertyRepository;
    private final PropertyInquiryRepository inquiryRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public SiteVisitResponse schedule(ScheduleVisitRequest req, UUID buyerId) {
        SaleProperty sp = salePropertyRepository.findById(req.salePropertyId())
                .orElseThrow(() -> new RuntimeException("Sale property not found"));

        SiteVisit visit = SiteVisit.builder()
                .inquiryId(req.inquiryId())
                .salePropertyId(req.salePropertyId())
                .buyerId(buyerId)
                .sellerId(sp.getSellerId())
                .scheduledAt(req.scheduledAt())
                .durationMinutes(req.durationMinutes() != null ? req.durationMinutes() : 30)
                .build();

        visit = visitRepository.save(visit);

        // Update inquiry status if linked
        if (req.inquiryId() != null) {
            inquiryRepository.findById(req.inquiryId()).ifPresent(inq -> {
                inq.setStatus(InquiryStatus.VISIT_SCHEDULED);
                inquiryRepository.save(inq);
            });
        }

        kafkaTemplate.send("sale.visit.scheduled", visit.getId().toString(), visit);
        log.info("Site visit {} scheduled for property {} at {}", visit.getId(), sp.getId(), req.scheduledAt());
        return toResponse(visit, sp);
    }

    @Transactional
    public SiteVisitResponse updateStatus(UUID visitId, VisitStatus status, UUID userId) {
        SiteVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found: " + visitId));
        if (!visit.getSellerId().equals(userId) && !visit.getBuyerId().equals(userId)) {
            throw new RuntimeException("Not authorized");
        }
        visit.setStatus(status);
        visit = visitRepository.save(visit);

        // Update inquiry if visit completed
        if (status == VisitStatus.COMPLETED && visit.getInquiryId() != null) {
            inquiryRepository.findById(visit.getInquiryId()).ifPresent(inq -> {
                inq.setStatus(InquiryStatus.VISIT_COMPLETED);
                inquiryRepository.save(inq);
            });
        }

        if (status == VisitStatus.CANCELLED) {
            kafkaTemplate.send("sale.visit.cancelled", visit.getId().toString(), visit);
        }
        return toResponseWithProperty(visit);
    }

    @Transactional
    public SiteVisitResponse addFeedback(UUID visitId, String feedback, Integer rating, UUID userId) {
        SiteVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new RuntimeException("Visit not found"));

        if (visit.getBuyerId().equals(userId)) {
            visit.setBuyerFeedback(feedback);
            if (rating != null) visit.setRating(rating);
        } else if (visit.getSellerId().equals(userId)) {
            visit.setSellerFeedback(feedback);
        } else {
            throw new RuntimeException("Not authorized");
        }
        visit = visitRepository.save(visit);
        return toResponseWithProperty(visit);
    }

    public Page<SiteVisitResponse> getBuyerVisits(UUID buyerId, Pageable pageable) {
        return visitRepository.findByBuyerIdOrderByScheduledAtDesc(buyerId, pageable)
                .map(this::toResponseWithProperty);
    }

    public Page<SiteVisitResponse> getSellerVisits(UUID sellerId, Pageable pageable) {
        return visitRepository.findBySellerIdOrderByScheduledAtDesc(sellerId, pageable)
                .map(this::toResponseWithProperty);
    }

    public List<SiteVisitResponse> getUpcomingForSeller(UUID sellerId) {
        return visitRepository.findUpcomingVisitsForSeller(sellerId, OffsetDateTime.now())
                .stream().map(this::toResponseWithProperty).toList();
    }

    public List<SiteVisitResponse> getUpcomingForBuyer(UUID buyerId) {
        return visitRepository.findUpcomingVisitsForBuyer(buyerId, OffsetDateTime.now())
                .stream().map(this::toResponseWithProperty).toList();
    }

    private SiteVisitResponse toResponseWithProperty(SiteVisit v) {
        SaleProperty sp = salePropertyRepository.findById(v.getSalePropertyId()).orElse(null);
        return toResponse(v, sp);
    }

    private SiteVisitResponse toResponse(SiteVisit v, SaleProperty sp) {
        return new SiteVisitResponse(
                v.getId(), v.getInquiryId(), v.getSalePropertyId(),
                v.getBuyerId(), v.getSellerId(),
                v.getScheduledAt(), v.getDurationMinutes(), v.getStatus(),
                v.getBuyerFeedback(), v.getSellerFeedback(), v.getRating(),
                sp != null ? sp.getTitle() : null,
                sp != null ? sp.getLocality() : null,
                sp != null ? sp.getCity() : null,
                v.getCreatedAt(), v.getUpdatedAt()
        );
    }
}
