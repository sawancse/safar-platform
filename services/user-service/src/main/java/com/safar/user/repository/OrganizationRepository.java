package com.safar.user.repository;

import com.safar.user.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrganizationRepository extends JpaRepository<Organization, UUID> {
    List<Organization> findByActive(boolean active);
    List<Organization> findByType(String type);
}
