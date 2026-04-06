package com.safar.listing.service;

import com.safar.listing.dto.CreateInquiryRequest;
import com.safar.listing.dto.InquiryResponse;
import com.safar.listing.entity.BuilderProject;
import com.safar.listing.entity.PropertyInquiry;
import com.safar.listing.entity.SaleProperty;
import com.safar.listing.entity.enums.InquiryStatus;
import com.safar.listing.repository.BuilderProjectRepository;
import com.safar.listing.repository.PropertyInquiryRepository;
import com.safar.listing.repository.SalePropertyRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyInquiryService {

    private final PropertyInquiryRepository inquiryRepository;
    private final SalePropertyRepository salePropertyRepository;
    private final BuilderProjectRepository builderProjectRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("Failed to serialize to JSON", e);
            return "{}";
        }
    }

    @Transactional
    public InquiryResponse create(CreateInquiryRequest req, UUID buyerId) {
        // Builder project inquiry
        if (req.builderProjectId() != null) {
            return createBuilderInquiry(req, buyerId);
        }

        // Sale property inquiry
        if (req.salePropertyId() == null) {
            throw new IllegalArgumentException("Either salePropertyId or builderProjectId is required");
        }

        SaleProperty sp = salePropertyRepository.findById(req.salePropertyId())
                .orElseThrow(() -> new RuntimeException("Sale property not found: " + req.salePropertyId()));

        if (inquiryRepository.existsByBuyerIdAndSalePropertyId(buyerId, req.salePropertyId())) {
            throw new RuntimeException("You have already sent an inquiry for this property");
        }

        PropertyInquiry inquiry = PropertyInquiry.builder()
                .salePropertyId(req.salePropertyId())
                .buyerId(buyerId)
                .sellerId(sp.getSellerId())
                .message(req.message())
                .buyerName(req.buyerName())
                .buyerPhone(req.buyerPhone())
                .buyerEmail(req.buyerEmail())
                .preferredVisitDate(req.preferredVisitDate())
                .preferredVisitTime(req.preferredVisitTime())
                .financingType(req.financingType())
                .budgetMinPaise(req.budgetMinPaise())
                .budgetMaxPaise(req.budgetMaxPaise())
                .build();

        inquiry = inquiryRepository.save(inquiry);

        sp.setInquiriesCount(sp.getInquiriesCount() + 1);
        salePropertyRepository.save(sp);

        kafkaTemplate.send("sale.inquiry.new", inquiry.getId().toString(), toJson(inquiry));
        log.info("Inquiry {} created for property {} by buyer {}", inquiry.getId(), sp.getId(), buyerId);
        return toResponse(inquiry, sp);
    }

    private InquiryResponse createBuilderInquiry(CreateInquiryRequest req, UUID buyerId) {
        BuilderProject bp = builderProjectRepository.findById(req.builderProjectId())
                .orElseThrow(() -> new RuntimeException("Builder project not found: " + req.builderProjectId()));

        PropertyInquiry inquiry = PropertyInquiry.builder()
                .builderProjectId(req.builderProjectId())
                .buyerId(buyerId)
                .sellerId(bp.getBuilderId())
                .message(req.message())
                .buyerName(req.buyerName())
                .buyerPhone(req.buyerPhone())
                .buyerEmail(req.buyerEmail())
                .preferredVisitDate(req.preferredVisitDate())
                .preferredVisitTime(req.preferredVisitTime())
                .financingType(req.financingType())
                .budgetMinPaise(req.budgetMinPaise())
                .budgetMaxPaise(req.budgetMaxPaise())
                .build();

        inquiry = inquiryRepository.save(inquiry);
        kafkaTemplate.send("sale.inquiry.new", inquiry.getId().toString(), toJson(inquiry));
        log.info("Inquiry {} created for builder project {} by buyer {}", inquiry.getId(), bp.getId(), buyerId);
        return toResponse(inquiry, null);
    }

    public Page<InquiryResponse> getBuyerInquiries(UUID buyerId, Pageable pageable) {
        return inquiryRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(this::toResponseWithProperty);
    }

    public Page<InquiryResponse> getSellerInquiries(UUID sellerId, Pageable pageable) {
        return inquiryRepository.findBySellerIdOrderByCreatedAtDesc(sellerId, pageable)
                .map(this::toResponseWithProperty);
    }

    public Page<InquiryResponse> getPropertyInquiries(UUID salePropertyId, Pageable pageable) {
        return inquiryRepository.findBySalePropertyIdOrderByCreatedAtDesc(salePropertyId, pageable)
                .map(this::toResponseWithProperty);
    }

    @Transactional
    public InquiryResponse updateStatus(UUID inquiryId, InquiryStatus status, UUID userId) {
        PropertyInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found: " + inquiryId));
        if (!inquiry.getSellerId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this inquiry");
        }
        inquiry.setStatus(status);
        inquiry = inquiryRepository.save(inquiry);
        return toResponseWithProperty(inquiry);
    }

    @Transactional
    public InquiryResponse addNote(UUID inquiryId, String note, UUID sellerId) {
        PropertyInquiry inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new RuntimeException("Inquiry not found: " + inquiryId));
        if (!inquiry.getSellerId().equals(sellerId)) {
            throw new RuntimeException("Not authorized");
        }
        inquiry.setNotes(note);
        inquiry = inquiryRepository.save(inquiry);
        return toResponseWithProperty(inquiry);
    }

    private InquiryResponse toResponseWithProperty(PropertyInquiry inq) {
        SaleProperty sp = inq.getSalePropertyId() != null
                ? salePropertyRepository.findById(inq.getSalePropertyId()).orElse(null)
                : null;
        // For builder inquiries, fill property title from builder project
        if (sp == null && inq.getBuilderProjectId() != null) {
            BuilderProject bp = builderProjectRepository.findById(inq.getBuilderProjectId()).orElse(null);
            if (bp != null) {
                return new InquiryResponse(
                        inq.getId(), inq.getSalePropertyId(), inq.getBuyerId(), inq.getSellerId(),
                        inq.getStatus(), inq.getMessage(),
                        inq.getBuyerName(), inq.getBuyerPhone(), inq.getBuyerEmail(),
                        inq.getPreferredVisitDate(), inq.getPreferredVisitTime(),
                        inq.getFinancingType(), inq.getBudgetMinPaise(), inq.getBudgetMaxPaise(),
                        inq.getNotes(),
                        bp.getProjectName(), bp.getLocality(), bp.getCity(), null,
                        inq.getCreatedAt(), inq.getUpdatedAt()
                );
            }
        }
        return toResponse(inq, sp);
    }

    private InquiryResponse toResponse(PropertyInquiry inq, SaleProperty sp) {
        return new InquiryResponse(
                inq.getId(), inq.getSalePropertyId(), inq.getBuyerId(), inq.getSellerId(),
                inq.getStatus(), inq.getMessage(),
                inq.getBuyerName(), inq.getBuyerPhone(), inq.getBuyerEmail(),
                inq.getPreferredVisitDate(), inq.getPreferredVisitTime(),
                inq.getFinancingType(), inq.getBudgetMinPaise(), inq.getBudgetMaxPaise(),
                inq.getNotes(),
                sp != null ? sp.getTitle() : null,
                sp != null ? sp.getLocality() : null,
                sp != null ? sp.getCity() : null,
                sp != null ? sp.getAskingPricePaise() : null,
                inq.getCreatedAt(), inq.getUpdatedAt()
        );
    }
}
