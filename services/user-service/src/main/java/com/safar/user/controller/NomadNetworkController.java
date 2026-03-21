package com.safar.user.controller;

import com.safar.user.dto.CommentRequest;
import com.safar.user.dto.NomadCommentResponse;
import com.safar.user.dto.NomadPostRequest;
import com.safar.user.dto.NomadPostResponse;
import com.safar.user.dto.SkillSwapRequest;
import com.safar.user.entity.NomadConnection;
import com.safar.user.entity.SkillSwap;
import com.safar.user.service.NomadNetworkService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/nomad")
@RequiredArgsConstructor
public class NomadNetworkController {

    private final NomadNetworkService service;

    @PostMapping("/posts")
    public ResponseEntity<NomadPostResponse> createPost(Authentication auth,
                                                        @RequestBody NomadPostRequest req) {
        UUID authorId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.toPostResponse(service.createPost(authorId, req)));
    }

    @GetMapping("/posts")
    public ResponseEntity<List<NomadPostResponse>> getCityFeed(@RequestParam String city,
                                                               @RequestParam(required = false) String category,
                                                               Pageable pageable) {
        List<NomadPostResponse> posts = service.getCityFeed(city, category, pageable)
                .map(service::toPostResponse)
                .getContent();
        return ResponseEntity.ok(posts);
    }

    @GetMapping("/feed")
    public ResponseEntity<List<NomadPostResponse>> getFeed(@RequestParam String city,
                                                           @RequestParam(required = false) String category,
                                                           Pageable pageable) {
        return getCityFeed(city, category, pageable);
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<NomadPostResponse> getPost(@PathVariable UUID id) {
        return ResponseEntity.ok(service.toPostResponse(service.getPost(id)));
    }

    @PostMapping("/posts/{id}/upvote")
    public ResponseEntity<NomadPostResponse> upvotePost(@PathVariable UUID id) {
        return ResponseEntity.ok(service.toPostResponse(service.upvotePost(id)));
    }

    @GetMapping("/posts/{id}/comments")
    public ResponseEntity<List<NomadCommentResponse>> getComments(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getComments(id));
    }

    @PostMapping("/posts/{id}/comments")
    public ResponseEntity<NomadCommentResponse> addComment(Authentication auth,
                                                           @PathVariable UUID id,
                                                           @RequestBody CommentRequest req) {
        UUID authorId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.toCommentResponse(service.addComment(authorId, id, req.body())));
    }

    @PostMapping("/connections/{userId}")
    public ResponseEntity<NomadConnection> sendConnectionRequest(Authentication auth,
                                                                  @PathVariable UUID userId) {
        UUID requesterId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.sendConnectionRequest(requesterId, userId));
    }

    @PutMapping("/connections/{id}/accept")
    public ResponseEntity<NomadConnection> acceptConnection(Authentication auth,
                                                             @PathVariable UUID id) {
        UUID recipientId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.acceptConnection(recipientId, id));
    }

    @GetMapping("/connections")
    public ResponseEntity<List<NomadConnection>> getMyConnections(Authentication auth) {
        UUID userId = UUID.fromString(auth.getName());
        return ResponseEntity.ok(service.getMyConnections(userId));
    }

    @PostMapping("/skill-swaps")
    public ResponseEntity<SkillSwap> postSkillSwap(Authentication auth,
                                                    @RequestBody SkillSwapRequest req) {
        UUID posterId = UUID.fromString(auth.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.postSkillSwap(posterId, req));
    }

    @GetMapping("/skill-swaps")
    public ResponseEntity<Page<SkillSwap>> searchSkillSwaps(@RequestParam String city,
                                                             Pageable pageable) {
        return ResponseEntity.ok(service.searchSkillSwaps(city, pageable));
    }
}
