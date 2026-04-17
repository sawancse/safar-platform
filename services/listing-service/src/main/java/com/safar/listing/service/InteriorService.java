package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.*;
import com.safar.listing.entity.enums.*;
import com.safar.listing.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InteriorService {

    private final InteriorProjectRepository interiorProjectRepository;
    private final InteriorDesignerRepository interiorDesignerRepository;
    private final RoomDesignRepository roomDesignRepository;
    private final MaterialCatalogRepository materialCatalogRepository;
    private final MaterialSelectionRepository materialSelectionRepository;
    private final InteriorQuoteRepository interiorQuoteRepository;
    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    // ── Book Consultation ────────────────────────────────────

    @Transactional
    public InteriorProjectResponse bookConsultation(BookConsultationRequest req, UUID userId) {
        InteriorProject project = InteriorProject.builder()
                .userId(userId)
                .projectType(InteriorProjectType.valueOf(req.projectType()))
                .propertyAddress(req.propertyAddress())
                .city(req.city())
                .state(req.state())
                .budgetMinPaise(req.budgetMinPaise())
                .budgetMaxPaise(req.budgetMaxPaise())
                .status(InteriorProjectStatus.CONSULTATION_BOOKED)
                .build();

        project = interiorProjectRepository.save(project);
        log.info("Interior consultation booked: {} by user {}", project.getId(), userId);

        kafkaTemplate.send("interior.consultation.booked", project.getId().toString(), project.getId().toString());

        return toProjectResponse(project);
    }

    // ── Get Project ──────────────────────────────────────────

    public InteriorProjectResponse getProject(UUID id) {
        InteriorProject project = interiorProjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Interior project not found: " + id));
        return toProjectResponse(project);
    }

    // ── My Projects ──────────────────────────────────────────

    public Page<InteriorProjectResponse> getMyProjects(UUID userId, Pageable pageable) {
        return interiorProjectRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toProjectResponse);
    }

    // ���─ Assign Designer (Admin) ──────────────────────────────

    // ── Admin: List All Projects ─────────────────────────────

    public Page<InteriorProjectResponse> getAllProjects(String status, Pageable pageable) {
        Page<InteriorProject> page;
        if (status != null && !status.isBlank()) {
            page = interiorProjectRepository.findByStatusOrderByCreatedAtDesc(
                    InteriorProjectStatus.valueOf(status), pageable);
        } else {
            page = interiorProjectRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return page.map(this::toProjectResponse);
    }

    @Transactional
    public InteriorProjectResponse assignDesigner(UUID projectId, UUID designerId) {
        InteriorProject project = interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        interiorDesignerRepository.findById(designerId)
                .orElseThrow(() -> new RuntimeException("Designer not found: " + designerId));

        project.setDesignerId(designerId);
        project = interiorProjectRepository.save(project);

        log.info("Designer {} assigned to project {}", designerId, projectId);
        kafkaTemplate.send("interior.designer.assigned", projectId.toString(), designerId.toString());

        return toProjectResponse(project);
    }

    // ── Update Project Status ────────────────────────────────

    @Transactional
    public InteriorProjectResponse updateProjectStatus(UUID projectId, String status) {
        InteriorProject project = interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        project.setStatus(InteriorProjectStatus.valueOf(status));
        project = interiorProjectRepository.save(project);

        log.info("Project {} status updated to {}", projectId, status);

        return toProjectResponse(project);
    }

    // ── Add Room Design ──────────────────────────────────────

    @Transactional
    public RoomDesignResponse addRoomDesign(UUID projectId, RoomDesignRequest req) {
        interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        RoomDesign roomDesign = RoomDesign.builder()
                .interiorProjectId(projectId)
                .roomType(InteriorRoomType.valueOf(req.roomType()))
                .areaSqft(req.areaSqft())
                .designStyle(req.designStyle() != null ? DesignStyle.valueOf(req.designStyle()) : null)
                .approved(false)
                .build();

        roomDesign = roomDesignRepository.save(roomDesign);
        log.info("Room design added to project {}: {}", projectId, roomDesign.getId());

        return toRoomDesignResponse(roomDesign);
    }

    // ── Get Room Designs ─────────────────────────────────────

    public List<RoomDesignResponse> getRoomDesigns(UUID projectId) {
        return roomDesignRepository.findByInteriorProjectId(projectId)
                .stream()
                .map(this::toRoomDesignResponse)
                .toList();
    }

    // ── Approve Room Design ──────────────────────────────────

    @Transactional
    public RoomDesignResponse approveRoomDesign(UUID roomId) {
        RoomDesign roomDesign = roomDesignRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Room design not found: " + roomId));

        roomDesign.setApproved(true);
        roomDesign.setApprovedAt(OffsetDateTime.now());
        roomDesign = roomDesignRepository.save(roomDesign);

        log.info("Room design approved: {}", roomId);

        return toRoomDesignResponse(roomDesign);
    }

    // ── Add Material Selection ───────────────────────────────

    @Transactional
    public MaterialSelection addMaterialSelection(UUID projectId, MaterialSelectionRequest req) {
        interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        long totalPrice = req.unitPricePaise() != null && req.quantity() != null
                ? req.unitPricePaise() * req.quantity()
                : 0L;

        MaterialSelection selection = MaterialSelection.builder()
                .roomDesignId(req.roomDesignId())
                .materialCatalogId(req.materialId())
                .category(MaterialCategory.valueOf(req.category()))
                .quantity(req.quantity())
                .unitPricePaise(req.unitPricePaise())
                .totalPricePaise(totalPrice)
                .build();

        selection = materialSelectionRepository.save(selection);
        log.info("Material selection added for project {}: {}", projectId, selection.getId());

        return selection;
    }

    // ── Get Materials ────────────────────────────────────────

    public List<MaterialSelection> getMaterials(UUID projectId) {
        return materialSelectionRepository.findByProjectId(projectId);
    }

    // ── Browse Catalog ───────────────────────────────────────

    public List<MaterialCatalogResponse> browseCatalog(String category) {
        List<MaterialCatalog> items;
        if (category != null && !category.isBlank()) {
            items = materialCatalogRepository.findByCategoryAndActiveTrue(
                    MaterialCategory.valueOf(category.toUpperCase()));
        } else {
            items = materialCatalogRepository.findByActiveTrue();
        }

        return items.stream()
                .map(this::toCatalogResponse)
                .toList();
    }

    // ── Generate Quote ───────────────────────────────────────

    @Transactional
    public InteriorQuoteResponse generateQuote(UUID projectId) {
        InteriorProject project = interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        List<MaterialSelection> materials = materialSelectionRepository.findByProjectId(projectId);

        long materialCost = materials.stream()
                .mapToLong(m -> m.getTotalPricePaise() != null ? m.getTotalPricePaise() : 0L)
                .sum();

        // Labour = 30% of materials, Hardware + Overhead = 20% of materials
        long labourCost = materialCost * 30 / 100;
        long overheadCost = materialCost * 20 / 100; // 10% hardware + 10% overhead
        long taxPaise = (materialCost + labourCost + overheadCost) * 18 / 100; // 18% GST
        long total = materialCost + labourCost + overheadCost + taxPaise;

        // Determine version
        List<InteriorQuote> existing = interiorQuoteRepository.findByInteriorProjectIdOrderByCreatedAtDesc(projectId);
        int version = existing.isEmpty() ? 1 : existing.size() + 1;

        InteriorQuote quote = InteriorQuote.builder()
                .interiorProjectId(projectId)
                .quoteNumber("QT-" + projectId.toString().substring(0, 8) + "-V" + version)
                .status(QuoteStatus.DRAFT)
                .materialCostPaise(materialCost)
                .labourCostPaise(labourCost)
                .overheadCostPaise(overheadCost)
                .taxPaise(taxPaise)
                .discountPaise(0L)
                .totalAmountPaise(total)
                .validUntil(LocalDate.now().plusDays(30))
                .build();

        quote = interiorQuoteRepository.save(quote);

        // Update project quoted amount
        project.setQuotedAmountPaise(total);
        interiorProjectRepository.save(project);

        log.info("Quote generated for project {}: total={} paise (v{})", projectId, total, version);

        return toQuoteResponse(quote);
    }

    // ── Approve Quote ────────────────────────────────────────

    @Transactional
    public InteriorQuoteResponse approveQuote(UUID quoteId) {
        InteriorQuote quote = interiorQuoteRepository.findById(quoteId)
                .orElseThrow(() -> new RuntimeException("Quote not found: " + quoteId));

        quote.setStatus(QuoteStatus.APPROVED);
        quote.setApprovedAt(OffsetDateTime.now());
        quote = interiorQuoteRepository.save(quote);

        // Update project status
        InteriorProject project = interiorProjectRepository.findById(quote.getInteriorProjectId())
                .orElse(null);
        if (project != null) {
            project.setStatus(InteriorProjectStatus.QUOTE_APPROVED);
            project.setApprovedAmountPaise(quote.getTotalAmountPaise());
            interiorProjectRepository.save(project);
        }

        log.info("Quote approved: {}", quoteId);
        kafkaTemplate.send("interior.quote.approved", quoteId.toString(), quoteId.toString());

        return toQuoteResponse(quote);
    }

    // ── Add Milestone ────────────────────────────────────────

    @Transactional
    public MilestoneResponse addMilestone(UUID projectId, String name, String description,
                                          LocalDate scheduledDate, Long paymentAmountPaise) {
        interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        List<ProjectMilestone> existing = projectMilestoneRepository
                .findByInteriorProjectIdOrderByPlannedDateAsc(projectId);
        int sortOrder = existing.size() + 1;

        ProjectMilestone milestone = ProjectMilestone.builder()
                .interiorProjectId(projectId)
                .title(name)
                .description(description)
                .plannedDate(scheduledDate)
                .paymentAmountPaise(paymentAmountPaise)
                .paymentRequired(paymentAmountPaise != null && paymentAmountPaise > 0)
                .status(MilestoneStatus.PENDING)
                .sortOrder(sortOrder)
                .build();

        milestone = projectMilestoneRepository.save(milestone);
        log.info("Milestone added to project {}: {}", projectId, milestone.getId());

        return toMilestoneResponse(milestone);
    }

    // ── Complete Milestone ───────────────────────────────────

    @Transactional
    public MilestoneResponse completeMilestone(UUID milestoneId, String[] photos) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new RuntimeException("Milestone not found: " + milestoneId));

        milestone.setStatus(MilestoneStatus.COMPLETED);
        milestone.setCompletedDate(LocalDate.now());
        milestone.setCompletionPercent(BigDecimal.valueOf(100));
        if (photos != null) {
            milestone.setPhotos(photos);
        }

        milestone = projectMilestoneRepository.save(milestone);
        log.info("Milestone completed: {}", milestoneId);

        kafkaTemplate.send("interior.milestone.completed", milestoneId.toString(), milestoneId.toString());

        return toMilestoneResponse(milestone);
    }

    // ── Get Milestones ───────────────────────────────────────

    public List<MilestoneResponse> getMilestones(UUID projectId) {
        return projectMilestoneRepository.findByInteriorProjectIdOrderByPlannedDateAsc(projectId)
                .stream()
                .map(this::toMilestoneResponse)
                .toList();
    }

    // ── Add Quality Check ────────────────────────────────────

    @Transactional
    public QualityCheckResponse addQualityCheck(UUID projectId, UUID milestoneId,
                                                 String checkpointName, String category,
                                                 String status, String notes) {
        interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        QualityCheck qc = QualityCheck.builder()
                .interiorProjectId(projectId)
                .milestoneId(milestoneId)
                .checkName(checkpointName)
                .status(status != null ? QcStatus.valueOf(status) : QcStatus.PENDING)
                .findings(notes)
                .build();

        qc = qualityCheckRepository.save(qc);
        log.info("Quality check added to project {}: {}", projectId, qc.getId());

        return toQualityCheckResponse(qc);
    }

    // ── Get Quality Checks ───────────────────────────────────

    public List<QualityCheckResponse> getQualityChecks(UUID projectId) {
        return qualityCheckRepository.findByInteriorProjectId(projectId)
                .stream()
                .map(this::toQualityCheckResponse)
                .toList();
    }

    // ── List Designers ───────────────────────────────────────

    public List<DesignerResponse> listDesigners(String city) {
        List<InteriorDesigner> designers = (city != null && !city.isBlank())
                ? interiorDesignerRepository.findByCityAndActiveTrue(city)
                : interiorDesignerRepository.findByActiveTrue();

        return designers.stream()
                .map(this::toDesignerResponse)
                .toList();
    }

    // ── Submit Review ────────────────────────────────────────

    @Transactional
    public void submitReview(UUID projectId, String feedback, Integer rating, UUID userId) {
        InteriorProject project = interiorProjectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found: " + projectId));

        if (!project.getUserId().equals(userId)) {
            throw new RuntimeException("Not authorized to review this project");
        }

        if (project.getDesignerId() != null) {
            InteriorDesigner designer = interiorDesignerRepository.findById(project.getDesignerId())
                    .orElse(null);
            if (designer != null) {
                // Update designer rating (simple average approximation)
                BigDecimal currentRating = designer.getRating() != null
                        ? designer.getRating() : BigDecimal.ZERO;
                int completed = designer.getCompletedProjects() != null
                        ? designer.getCompletedProjects() : 0;
                BigDecimal newRating = currentRating.multiply(BigDecimal.valueOf(completed))
                        .add(BigDecimal.valueOf(rating))
                        .divide(BigDecimal.valueOf(completed + 1), 2, java.math.RoundingMode.HALF_UP);
                designer.setRating(newRating);
                designer.setCompletedProjects(completed + 1);
                interiorDesignerRepository.save(designer);
            }
        }

        project.setNotes(feedback);
        interiorProjectRepository.save(project);

        log.info("Review submitted for project {} by user {}: rating={}", projectId, userId, rating);
        kafkaTemplate.send("interior.review.submitted", projectId.toString(), String.valueOf(rating));
    }

    // ── Private Helpers ──────────────────────────────────────

    private InteriorProjectResponse toProjectResponse(InteriorProject p) {
        String designerName = null;
        String designerPhone = null;
        String designerEmail = null;
        if (p.getDesignerId() != null) {
            InteriorDesigner designer = interiorDesignerRepository.findById(p.getDesignerId())
                    .orElse(null);
            if (designer != null) {
                designerName = designer.getFullName();
                designerPhone = designer.getPhone();
                designerEmail = designer.getEmail();
            }
        }

        List<RoomDesign> rooms = roomDesignRepository.findByInteriorProjectId(p.getId());

        return new InteriorProjectResponse(
                p.getId(),
                p.getUserId(),
                p.getDesignerId(),
                designerName,
                designerPhone,
                designerEmail,
                p.getProjectType() != null ? p.getProjectType().name() : null,
                null, // propertyType
                p.getPropertyAddress(),
                p.getCity(),
                p.getState(),
                null, // pincode
                rooms.size() > 0 ? rooms.size() : null,
                p.getBudgetMinPaise(),
                p.getBudgetMaxPaise(),
                p.getExpectedStartDate(),
                p.getStatus(),
                rooms.size(),
                p.getQuotedAmountPaise(),
                p.getApprovedAmountPaise(),
                null, // paidAmountPaise — will be from payment-service later
                p.getNotes(),
                p.getExpectedStartDate(),
                p.getExpectedEndDate(),
                p.getActualStartDate(),
                p.getActualEndDate(),
                p.getCreatedAt(),
                p.getUpdatedAt()
        );
    }

    private RoomDesignResponse toRoomDesignResponse(RoomDesign r) {
        return new RoomDesignResponse(
                r.getId(),
                r.getInteriorProjectId(),
                r.getRoomType() != null ? r.getRoomType().name() : null,
                r.getAreaSqft(),
                r.getDesignStyle() != null ? r.getDesignStyle().name() : null,
                r.getDesign2dUrl(),
                r.getDesign3dUrl(),
                null, // render3dUrl
                r.getApproved() != null && r.getApproved() ? "APPROVED" : "PENDING",
                r.getFeedback(),
                r.getEstimatedCostPaise(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private MaterialCatalogResponse toCatalogResponse(MaterialCatalog m) {
        return new MaterialCatalogResponse(
                m.getId(),
                m.getCategory(),
                m.getName(),
                m.getBrand(),
                m.getDescription(),
                m.getPhotos() != null && m.getPhotos().length > 0 ? m.getPhotos()[0] : null,
                m.getPricePerUnitPaise(),
                m.getUnit(),
                m.getDimensions(),
                m.getWarranty() != null ? parseWarrantyYears(m.getWarranty()) : null,
                m.getActive(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }

    private InteriorQuoteResponse toQuoteResponse(InteriorQuote q) {
        // Determine version from quote number
        int version = 1;
        if (q.getQuoteNumber() != null && q.getQuoteNumber().contains("-V")) {
            try {
                version = Integer.parseInt(q.getQuoteNumber().substring(
                        q.getQuoteNumber().lastIndexOf('V') + 1));
            } catch (NumberFormatException ignored) {
            }
        }

        return new InteriorQuoteResponse(
                q.getId(),
                q.getInteriorProjectId(),
                version,
                q.getMaterialCostPaise(),
                q.getLabourCostPaise(),
                q.getOverheadCostPaise(),
                q.getTaxPaise(),
                q.getDiscountPaise(),
                q.getTotalAmountPaise(),
                q.getStatus() != null ? q.getStatus().name() : null,
                q.getNotes(),
                null, // quoteDocumentUrl
                q.getValidUntil() != null ? q.getValidUntil().atStartOfDay().atOffset(java.time.ZoneOffset.UTC) : null,
                q.getApprovedAt(),
                q.getCreatedAt(),
                q.getUpdatedAt()
        );
    }

    private MilestoneResponse toMilestoneResponse(ProjectMilestone m) {
        return new MilestoneResponse(
                m.getId(),
                m.getInteriorProjectId(),
                m.getTitle(),
                m.getDescription(),
                m.getSortOrder(),
                m.getPlannedDate(),
                m.getCompletedDate(),
                m.getStatus() != null ? m.getStatus().name() : null,
                m.getPaymentAmountPaise(),
                m.getPaymentDone(),
                m.getCreatedAt(),
                m.getUpdatedAt()
        );
    }

    private QualityCheckResponse toQualityCheckResponse(QualityCheck qc) {
        return new QualityCheckResponse(
                qc.getId(),
                qc.getInteriorProjectId(),
                qc.getMilestoneId(),
                qc.getCheckName(),
                qc.getStatus() != null ? qc.getStatus().name() : null,
                qc.getInspectorName(),
                qc.getPhotos() != null ? Arrays.asList(qc.getPhotos()) : List.of(),
                qc.getFindings(),
                qc.getStatus() == QcStatus.PASS,
                qc.getReworkNotes(),
                qc.getInspectedAt(),
                qc.getCreatedAt()
        );
    }

    private DesignerResponse toDesignerResponse(InteriorDesigner d) {
        return new DesignerResponse(
                d.getId(),
                d.getFullName(),
                d.getCompanyName(),
                d.getPhone(),
                d.getEmail(),
                d.getCity(),
                d.getState(),
                d.getProfilePhotoUrl(),
                d.getPortfolioUrls() != null && d.getPortfolioUrls().length > 0
                        ? d.getPortfolioUrls()[0] : null,
                d.getExperienceYears(),
                d.getSpecializations() != null ? List.of(d.getSpecializations()) : List.of(),
                d.getRating(),
                d.getCompletedProjects(),
                d.getConsultationFeePaise(),
                d.getActive(),
                d.getCreatedAt(),
                d.getUpdatedAt()
        );
    }

    private Integer parseWarrantyYears(String warranty) {
        if (warranty == null) return null;
        try {
            return Integer.parseInt(warranty.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
