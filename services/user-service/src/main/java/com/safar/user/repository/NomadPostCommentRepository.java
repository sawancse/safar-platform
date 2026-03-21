package com.safar.user.repository;

import com.safar.user.entity.NomadPostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NomadPostCommentRepository extends JpaRepository<NomadPostComment, UUID> {
    List<NomadPostComment> findByPostIdOrderByCreatedAtAsc(UUID postId);
    long countByPostId(UUID postId);
}
