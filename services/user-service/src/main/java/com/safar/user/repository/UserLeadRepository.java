package com.safar.user.repository;

import com.safar.user.entity.UserLead;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserLeadRepository extends JpaRepository<UserLead, UUID> {

    Optional<UserLead> findByEmail(String email);

    boolean existsByEmail(String email);

    Page<UserLead> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<UserLead> findByCityIgnoreCaseOrderByCreatedAtDesc(String city, Pageable pageable);

    long countByCreatedAtAfter(OffsetDateTime after);

    long countByConvertedTrue();
}
