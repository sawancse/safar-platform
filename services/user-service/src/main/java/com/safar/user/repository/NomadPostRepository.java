package com.safar.user.repository;

import com.safar.user.entity.NomadPost;
import com.safar.user.entity.enums.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NomadPostRepository extends JpaRepository<NomadPost, UUID> {
    Page<NomadPost> findByCity(String city, Pageable pageable);
    Page<NomadPost> findByCityAndCategory(String city, PostCategory category, Pageable pageable);
}
