package com.safar.listing.repository;

import com.safar.listing.entity.InteriorProject;
import com.safar.listing.entity.enums.InteriorProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InteriorProjectRepository extends JpaRepository<InteriorProject, UUID> {

    Page<InteriorProject> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<InteriorProject> findByDesignerId(UUID designerId);

    List<InteriorProject> findByStatus(InteriorProjectStatus status);

    Page<InteriorProject> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<InteriorProject> findByStatusOrderByCreatedAtDesc(InteriorProjectStatus status, Pageable pageable);
}
