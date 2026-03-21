package com.safar.user.service;

import com.safar.user.dto.NomadCommentResponse;
import com.safar.user.dto.NomadPostRequest;
import com.safar.user.dto.NomadPostResponse;
import com.safar.user.dto.SkillSwapRequest;
import com.safar.user.entity.NomadConnection;
import com.safar.user.entity.NomadPost;
import com.safar.user.entity.NomadPostComment;
import com.safar.user.entity.SkillSwap;
import com.safar.user.entity.UserProfile;
import com.safar.user.entity.enums.ConnectionStatus;
import com.safar.user.entity.enums.PostCategory;
import com.safar.user.repository.NomadConnectionRepository;
import com.safar.user.repository.NomadPostCommentRepository;
import com.safar.user.repository.NomadPostRepository;
import com.safar.user.repository.ProfileRepository;
import com.safar.user.repository.SkillSwapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NomadNetworkService {

    private final NomadPostRepository postRepo;
    private final NomadPostCommentRepository commentRepo;
    private final NomadConnectionRepository connectionRepo;
    private final SkillSwapRepository skillSwapRepo;
    private final ProfileRepository profileRepo;

    @Transactional
    public NomadPost createPost(UUID authorId, NomadPostRequest req) {
        NomadPost post = NomadPost.builder()
                .authorId(authorId)
                .city(req.city())
                .category(PostCategory.valueOf(req.category()))
                .title(req.title())
                .body(req.body())
                .tags(req.tags() != null ? req.tags() : "")
                .build();
        return postRepo.save(post);
    }

    public Page<NomadPost> getCityFeed(String city, String category, Pageable pageable) {
        if (category != null && !category.isBlank()) {
            return postRepo.findByCityAndCategory(city, PostCategory.valueOf(category), pageable);
        }
        return postRepo.findByCity(city, pageable);
    }

    public NomadPost getPost(UUID postId) {
        return postRepo.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));
    }

    @Transactional
    public NomadPostComment addComment(UUID authorId, UUID postId, String body) {
        if (!postRepo.existsById(postId)) {
            throw new NoSuchElementException("Post not found");
        }
        NomadPostComment comment = NomadPostComment.builder()
                .postId(postId)
                .authorId(authorId)
                .body(body)
                .build();
        return commentRepo.save(comment);
    }

    @Transactional
    public NomadPost upvotePost(UUID postId) {
        NomadPost post = postRepo.findById(postId)
                .orElseThrow(() -> new NoSuchElementException("Post not found"));
        post.setUpvotes(post.getUpvotes() + 1);
        return postRepo.save(post);
    }

    @Transactional
    public NomadConnection sendConnectionRequest(UUID requesterId, UUID recipientId) {
        if (requesterId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot connect with yourself");
        }
        if (connectionRepo.existsByRequesterIdAndRecipientId(requesterId, recipientId)) {
            throw new IllegalStateException("Connection request already exists");
        }
        NomadConnection connection = NomadConnection.builder()
                .requesterId(requesterId)
                .recipientId(recipientId)
                .status(ConnectionStatus.PENDING)
                .build();
        return connectionRepo.save(connection);
    }

    @Transactional
    public NomadConnection acceptConnection(UUID recipientId, UUID connectionId) {
        NomadConnection connection = connectionRepo.findByIdAndRecipientId(connectionId, recipientId)
                .orElseThrow(() -> new NoSuchElementException("Connection request not found"));
        connection.setStatus(ConnectionStatus.ACCEPTED);
        return connectionRepo.save(connection);
    }

    public List<NomadConnection> getMyConnections(UUID userId) {
        return connectionRepo.findByRequesterIdAndStatusOrRecipientIdAndStatus(
                userId, ConnectionStatus.ACCEPTED,
                userId, ConnectionStatus.ACCEPTED);
    }

    @Transactional
    public SkillSwap postSkillSwap(UUID posterId, SkillSwapRequest req) {
        SkillSwap swap = SkillSwap.builder()
                .posterId(posterId)
                .offering(req.offering())
                .seeking(req.seeking())
                .city(req.city())
                .build();
        return skillSwapRepo.save(swap);
    }

    public Page<SkillSwap> searchSkillSwaps(String city, Pageable pageable) {
        return skillSwapRepo.findByCity(city, pageable);
    }

    public List<NomadCommentResponse> getComments(UUID postId) {
        return commentRepo.findByPostIdOrderByCreatedAtAsc(postId).stream()
                .map(this::toCommentResponse)
                .toList();
    }

    public NomadPostResponse toPostResponse(NomadPost post) {
        String authorName = profileRepo.findById(post.getAuthorId())
                .map(UserProfile::getName)
                .orElse("Unknown");
        long commentCount = commentRepo.countByPostId(post.getId());
        return new NomadPostResponse(
                post.getId(), post.getAuthorId(), authorName,
                post.getTitle(), post.getBody(), post.getCategory(),
                post.getCity(), post.getUpvotes(), commentCount,
                post.getCreatedAt()
        );
    }

    public NomadCommentResponse toCommentResponse(NomadPostComment comment) {
        String authorName = profileRepo.findById(comment.getAuthorId())
                .map(UserProfile::getName)
                .orElse("Unknown");
        return new NomadCommentResponse(
                comment.getId(), comment.getPostId(), comment.getAuthorId(),
                authorName, comment.getBody(), comment.getCreatedAt()
        );
    }
}
