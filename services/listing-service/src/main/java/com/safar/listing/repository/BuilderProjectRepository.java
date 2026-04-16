package com.safar.listing.repository;

import com.safar.listing.entity.BuilderProject;
import com.safar.listing.entity.enums.BuilderListingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BuilderProjectRepository extends JpaRepository<BuilderProject, UUID>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<BuilderProject> {

    Page<BuilderProject> findByBuilderId(UUID builderId, Pageable pageable);

    Page<BuilderProject> findByBuilderIdAndStatus(UUID builderId, BuilderListingStatus status, Pageable pageable);

    List<BuilderProject> findByStatus(BuilderListingStatus status);

    Page<BuilderProject> findByStatus(BuilderListingStatus status, Pageable pageable);

    Page<BuilderProject> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<BuilderProject> findByStatusOrderByCreatedAtDesc(BuilderListingStatus status, Pageable pageable);

    @Query("SELECT bp FROM BuilderProject bp WHERE bp.status = 'ACTIVE' " +
            "AND (:state IS NULL OR LOWER(bp.state) = LOWER(:state)) " +
            "AND (:city IS NULL OR LOWER(bp.city) = LOWER(:city)) " +
            "AND (:locality IS NULL OR LOWER(bp.locality) LIKE LOWER(CONCAT('%', :locality, '%'))) " +
            "AND (:minPrice IS NULL OR bp.maxPricePaise >= :minPrice) " +
            "AND (:maxPrice IS NULL OR bp.minPricePaise <= :maxPrice) " +
            "AND (:minBhk IS NULL OR bp.maxBhk >= :minBhk)")
    Page<BuilderProject> searchProjects(
            @Param("state") String state,
            @Param("city") String city,
            @Param("locality") String locality,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            @Param("minBhk") Integer minBhk,
            Pageable pageable);
}
