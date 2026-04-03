package com.safar.listing.service;

import com.safar.listing.dto.*;
import com.safar.listing.entity.BuilderProject;
import com.safar.listing.entity.ConstructionUpdate;
import com.safar.listing.entity.ProjectUnitType;
import com.safar.listing.entity.enums.BuilderListingStatus;
import com.safar.listing.repository.BuilderProjectRepository;
import com.safar.listing.repository.ConstructionUpdateRepository;
import com.safar.listing.repository.ProjectUnitTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BuilderProjectService {

    private final BuilderProjectRepository projectRepository;
    private final ProjectUnitTypeRepository unitTypeRepository;
    private final ConstructionUpdateRepository updateRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final GeocodingService geocodingService;

    // ── Project CRUD ──────────────────────────────────────────

    @Transactional
    public BuilderProjectResponse createProject(CreateBuilderProjectRequest req, UUID builderId) {
        BuilderProject project = BuilderProject.builder()
                .builderId(builderId)
                .builderName(req.builderName())
                .builderLogoUrl(req.builderLogoUrl())
                .projectName(req.projectName())
                .tagline(req.tagline())
                .description(req.description())
                .reraId(req.reraId())
                .city(req.city())
                .state(req.state())
                .locality(req.locality())
                .pincode(req.pincode())
                .lat(req.lat())
                .lng(req.lng())
                .address(req.address())
                .totalUnits(req.totalUnits())
                .availableUnits(req.totalUnits())
                .totalTowers(req.totalTowers())
                .totalFloorsMax(req.totalFloorsMax())
                .projectStatus(req.projectStatus())
                .launchDate(req.launchDate())
                .possessionDate(req.possessionDate())
                .constructionProgressPercent(req.constructionProgressPercent() != null ? req.constructionProgressPercent() : 0)
                .landAreaSqft(req.landAreaSqft())
                .projectAreaSqft(req.projectAreaSqft())
                .amenities(req.amenities())
                .masterPlanUrl(req.masterPlanUrl())
                .brochureUrl(req.brochureUrl())
                .walkthroughUrl(req.walkthroughUrl())
                .photos(req.photos())
                .bankApprovals(req.bankApprovals())
                .paymentPlansJson(req.paymentPlansJson())
                .build();

        // Geocode if missing
        if (project.getLat() == null && project.getPincode() != null) {
            try {
                var coords = geocodingService.geocode(project.getPincode(), project.getCity(), project.getState());
                if (coords != null && coords.length >= 2) {
                    project.setLat(coords[0]);
                    project.setLng(coords[1]);
                }
            } catch (Exception e) {
                log.warn("Geocoding failed: {}", e.getMessage());
            }
        }

        project = projectRepository.save(project);
        log.info("Builder project created: {} by builder {}", project.getId(), builderId);
        return toResponse(project);
    }

    public Page<BuilderProjectResponse> browseProjects(String city, Pageable pageable) {
        Page<BuilderProject> projects = (city != null && !city.isBlank())
                ? projectRepository.searchProjects(city, null, null, null, null, pageable)
                : projectRepository.findByStatus(BuilderListingStatus.ACTIVE, pageable);
        return projects.map(this::toResponse);
    }

    public BuilderProjectResponse getById(UUID id) {
        BuilderProject project = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Project not found: " + id));
        project.setViewsCount(project.getViewsCount() + 1);
        projectRepository.save(project);
        return toResponse(project);
    }

    @Transactional
    public BuilderProjectResponse updateProject(UUID id, CreateBuilderProjectRequest req, UUID builderId) {
        BuilderProject p = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found: " + id));
        if (!p.getBuilderId().equals(builderId)) throw new RuntimeException("Not authorized");

        if (req.projectName() != null) p.setProjectName(req.projectName());
        if (req.tagline() != null) p.setTagline(req.tagline());
        if (req.description() != null) p.setDescription(req.description());
        if (req.reraId() != null) p.setReraId(req.reraId());
        if (req.city() != null) p.setCity(req.city());
        if (req.state() != null) p.setState(req.state());
        if (req.locality() != null) p.setLocality(req.locality());
        if (req.pincode() != null) p.setPincode(req.pincode());
        if (req.address() != null) p.setAddress(req.address());
        if (req.totalUnits() != null) p.setTotalUnits(req.totalUnits());
        if (req.totalTowers() != null) p.setTotalTowers(req.totalTowers());
        if (req.totalFloorsMax() != null) p.setTotalFloorsMax(req.totalFloorsMax());
        if (req.projectStatus() != null) p.setProjectStatus(req.projectStatus());
        if (req.launchDate() != null) p.setLaunchDate(req.launchDate());
        if (req.possessionDate() != null) p.setPossessionDate(req.possessionDate());
        if (req.constructionProgressPercent() != null) p.setConstructionProgressPercent(req.constructionProgressPercent());
        if (req.amenities() != null) p.setAmenities(req.amenities());
        if (req.masterPlanUrl() != null) p.setMasterPlanUrl(req.masterPlanUrl());
        if (req.brochureUrl() != null) p.setBrochureUrl(req.brochureUrl());
        if (req.walkthroughUrl() != null) p.setWalkthroughUrl(req.walkthroughUrl());
        if (req.photos() != null) p.setPhotos(req.photos());
        if (req.bankApprovals() != null) p.setBankApprovals(req.bankApprovals());
        if (req.paymentPlansJson() != null) p.setPaymentPlansJson(req.paymentPlansJson());

        p = projectRepository.save(p);
        if (p.getStatus() == BuilderListingStatus.ACTIVE) {
            kafkaTemplate.send("builder.project.indexed", p.getId().toString(), p);
        }
        return toResponse(p);
    }

    @Transactional
    public BuilderProjectResponse publish(UUID id, UUID builderId) {
        BuilderProject p = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Not found"));
        if (!p.getBuilderId().equals(builderId)) throw new RuntimeException("Not authorized");
        p.setStatus(BuilderListingStatus.ACTIVE);
        p = projectRepository.save(p);
        kafkaTemplate.send("builder.project.indexed", p.getId().toString(), p);
        return toResponse(p);
    }

    public Page<BuilderProjectResponse> getBuilderProjects(UUID builderId, Pageable pageable) {
        return projectRepository.findByBuilderId(builderId, pageable).map(this::toResponse);
    }

    // ── Unit Types ────────────────────────────────────────────

    @Transactional
    public UnitTypeResponse addUnitType(UUID projectId, UnitTypeRequest req, UUID builderId) {
        BuilderProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getBuilderId().equals(builderId)) throw new RuntimeException("Not authorized");

        ProjectUnitType unit = ProjectUnitType.builder()
                .projectId(projectId)
                .name(req.name())
                .bhk(req.bhk())
                .carpetAreaSqft(req.carpetAreaSqft())
                .builtUpAreaSqft(req.builtUpAreaSqft())
                .superBuiltUpAreaSqft(req.superBuiltUpAreaSqft())
                .basePricePaise(req.basePricePaise())
                .floorRisePaise(req.floorRisePaise() != null ? req.floorRisePaise() : 0L)
                .facingPremiumPaise(req.facingPremiumPaise() != null ? req.facingPremiumPaise() : 0L)
                .premiumFloorsFrom(req.premiumFloorsFrom())
                .totalUnits(req.totalUnits())
                .availableUnits(req.totalUnits())
                .bathrooms(req.bathrooms())
                .balconies(req.balconies())
                .furnishing(req.furnishing())
                .floorPlanUrl(req.floorPlanUrl())
                .unitLayoutUrl(req.unitLayoutUrl())
                .photos(req.photos())
                .build();

        unit = unitTypeRepository.save(unit);
        recomputeProjectPriceRange(projectId);
        return toUnitResponse(unit);
    }

    public List<UnitTypeResponse> getUnitTypes(UUID projectId) {
        return unitTypeRepository.findByProjectIdOrderByBhkAscBasePricePaiseAsc(projectId)
                .stream().map(this::toUnitResponse).toList();
    }

    @Transactional
    public void deleteUnitType(UUID unitTypeId, UUID builderId) {
        ProjectUnitType unit = unitTypeRepository.findById(unitTypeId)
                .orElseThrow(() -> new RuntimeException("Unit type not found"));
        BuilderProject project = projectRepository.findById(unit.getProjectId())
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getBuilderId().equals(builderId)) throw new RuntimeException("Not authorized");
        unitTypeRepository.delete(unit);
        recomputeProjectPriceRange(unit.getProjectId());
    }

    // ── Price Calculator ──────────────────────────────────────

    public UnitPriceCalculation calculateUnitPrice(UUID unitTypeId, int floor, boolean preferredFacing) {
        ProjectUnitType unit = unitTypeRepository.findById(unitTypeId)
                .orElseThrow(() -> new RuntimeException("Unit type not found"));

        long price = unit.getBasePricePaise();

        // Floor rise
        long floorRise = 0;
        if (unit.getFloorRisePaise() != null && unit.getFloorRisePaise() > 0) {
            int riseFrom = unit.getPremiumFloorsFrom() != null ? unit.getPremiumFloorsFrom() : 1;
            if (floor > riseFrom) {
                floorRise = unit.getFloorRisePaise() * (floor - riseFrom);
            }
        }
        price += floorRise;

        // Facing premium
        long facingPremium = 0;
        if (preferredFacing && unit.getFacingPremiumPaise() != null) {
            facingPremium = unit.getFacingPremiumPaise();
        }
        price += facingPremium;

        // Price per sqft
        int area = unit.getCarpetAreaSqft() != null ? unit.getCarpetAreaSqft()
                : unit.getBuiltUpAreaSqft() != null ? unit.getBuiltUpAreaSqft() : 0;
        long pricePerSqft = area > 0 ? price / area : 0;

        // EMI estimate at 8.5% for 20 years
        long emi = calculateEmi(price, 8.5, 20);

        return new UnitPriceCalculation(
                unit.getName(), unit.getBhk(), unit.getBasePricePaise(),
                floor, floorRise, preferredFacing, facingPremium,
                price, pricePerSqft, emi
        );
    }

    // ── Construction Updates ──────────────────────────────────

    @Transactional
    public ConstructionUpdateResponse addConstructionUpdate(UUID projectId, ConstructionUpdateRequest req, UUID builderId) {
        BuilderProject project = projectRepository.findById(projectId)
                .orElseThrow(() -> new RuntimeException("Project not found"));
        if (!project.getBuilderId().equals(builderId)) throw new RuntimeException("Not authorized");

        ConstructionUpdate update = ConstructionUpdate.builder()
                .projectId(projectId)
                .title(req.title())
                .description(req.description())
                .progressPercent(req.progressPercent())
                .photos(req.photos())
                .build();

        update = updateRepository.save(update);

        // Update project progress
        if (req.progressPercent() != null) {
            project.setConstructionProgressPercent(req.progressPercent());
            projectRepository.save(project);
        }

        kafkaTemplate.send("builder.construction.update", projectId.toString(), update);
        return toUpdateResponse(update);
    }

    public List<ConstructionUpdateResponse> getConstructionUpdates(UUID projectId) {
        return updateRepository.findByProjectIdOrderByCreatedAtDesc(projectId)
                .stream().map(this::toUpdateResponse).toList();
    }

    // ── Admin ─────────────────────────────────────────────────

    public BuilderProjectResponse adminVerify(UUID id) {
        BuilderProject p = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        p.setVerified(true);
        p = projectRepository.save(p);
        return toResponse(p);
    }

    public BuilderProjectResponse adminVerifyRera(UUID id) {
        BuilderProject p = projectRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found"));
        p.setReraVerified(true);
        p = projectRepository.save(p);
        return toResponse(p);
    }

    public int reindexAll() {
        List<BuilderProject> active = projectRepository.findByStatus(BuilderListingStatus.ACTIVE);
        active.forEach(p -> kafkaTemplate.send("builder.project.indexed", p.getId().toString(), p));
        return active.size();
    }

    // ── Helpers ───────────────────────────────────────────────

    private void recomputeProjectPriceRange(UUID projectId) {
        List<ProjectUnitType> units = unitTypeRepository.findByProjectIdOrderByBhkAscBasePricePaiseAsc(projectId);
        if (units.isEmpty()) return;

        BuilderProject project = projectRepository.findById(projectId).orElse(null);
        if (project == null) return;

        project.setMinPricePaise(units.stream().mapToLong(ProjectUnitType::getBasePricePaise).min().orElse(0));
        project.setMaxPricePaise(units.stream().mapToLong(ProjectUnitType::getBasePricePaise).max().orElse(0));
        project.setMinBhk(units.stream().mapToInt(ProjectUnitType::getBhk).min().orElse(0));
        project.setMaxBhk(units.stream().mapToInt(ProjectUnitType::getBhk).max().orElse(0));
        project.setMinAreaSqft(units.stream()
                .filter(u -> u.getCarpetAreaSqft() != null)
                .mapToInt(ProjectUnitType::getCarpetAreaSqft).min().orElse(0));
        project.setMaxAreaSqft(units.stream()
                .filter(u -> u.getCarpetAreaSqft() != null)
                .mapToInt(ProjectUnitType::getCarpetAreaSqft).max().orElse(0));

        int totalUnits = units.stream().filter(u -> u.getTotalUnits() != null).mapToInt(ProjectUnitType::getTotalUnits).sum();
        int availableUnits = units.stream().filter(u -> u.getAvailableUnits() != null).mapToInt(ProjectUnitType::getAvailableUnits).sum();
        project.setTotalUnits(totalUnits);
        project.setAvailableUnits(availableUnits);

        projectRepository.save(project);
    }

    private long calculateEmi(long principalPaise, double annualRate, int years) {
        double r = annualRate / 12 / 100;
        int n = years * 12;
        double principal = principalPaise / 100.0;
        double emi = principal * r * Math.pow(1 + r, n) / (Math.pow(1 + r, n) - 1);
        return (long) (emi * 100); // back to paise
    }

    private BuilderProjectResponse toResponse(BuilderProject p) {
        List<UnitTypeResponse> units = unitTypeRepository.findByProjectIdOrderByBhkAscBasePricePaiseAsc(p.getId())
                .stream().map(this::toUnitResponse).toList();

        return new BuilderProjectResponse(
                p.getId(), p.getBuilderId(), p.getBuilderName(), p.getBuilderLogoUrl(),
                p.getProjectName(), p.getTagline(), p.getDescription(),
                p.getReraId(), p.getReraVerified(),
                p.getCity(), p.getState(), p.getLocality(), p.getPincode(),
                p.getLat(), p.getLng(), p.getAddress(),
                p.getTotalUnits(), p.getAvailableUnits(), p.getTotalTowers(), p.getTotalFloorsMax(),
                p.getProjectStatus(), p.getLaunchDate(), p.getPossessionDate(),
                p.getConstructionProgressPercent(),
                p.getLandAreaSqft(), p.getProjectAreaSqft(),
                p.getAmenities(), p.getMasterPlanUrl(), p.getBrochureUrl(), p.getWalkthroughUrl(),
                p.getPhotos(), p.getBankApprovals(), p.getPaymentPlansJson(),
                p.getMinPricePaise(), p.getMaxPricePaise(),
                p.getMinBhk(), p.getMaxBhk(), p.getMinAreaSqft(), p.getMaxAreaSqft(),
                p.getStatus(), p.getVerified(), p.getViewsCount(), p.getInquiriesCount(),
                units, p.getCreatedAt(), p.getUpdatedAt()
        );
    }

    private UnitTypeResponse toUnitResponse(ProjectUnitType u) {
        int area = u.getCarpetAreaSqft() != null ? u.getCarpetAreaSqft()
                : u.getBuiltUpAreaSqft() != null ? u.getBuiltUpAreaSqft() : 0;
        Long pricePerSqft = area > 0 ? u.getBasePricePaise() / area : null;

        return new UnitTypeResponse(
                u.getId(), u.getProjectId(), u.getName(), u.getBhk(),
                u.getCarpetAreaSqft(), u.getBuiltUpAreaSqft(), u.getSuperBuiltUpAreaSqft(),
                u.getBasePricePaise(), u.getFloorRisePaise(), u.getFacingPremiumPaise(),
                u.getPremiumFloorsFrom(),
                u.getTotalUnits(), u.getAvailableUnits(),
                u.getBathrooms(), u.getBalconies(), u.getFurnishing(),
                u.getFloorPlanUrl(), u.getUnitLayoutUrl(), u.getPhotos(),
                pricePerSqft
        );
    }

    private ConstructionUpdateResponse toUpdateResponse(ConstructionUpdate u) {
        return new ConstructionUpdateResponse(
                u.getId(), u.getProjectId(), u.getTitle(), u.getDescription(),
                u.getProgressPercent(), u.getPhotos(), u.getCreatedAt()
        );
    }
}
