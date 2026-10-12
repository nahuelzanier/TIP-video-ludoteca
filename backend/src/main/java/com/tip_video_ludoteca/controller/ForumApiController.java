package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.forums.CreateForumReplyRequest;
import com.tip_video_ludoteca.forums.ForumDiscussionService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.tip_video_ludoteca.forums.RateForumDiscussionRequest;
import org.springframework.web.bind.annotation.PutMapping;
import java.util.List;

@RestController
@RequestMapping("/api/forum/discussions")
public class ForumApiController {

    private final ForumDiscussionService discussions;

    public ForumApiController(ForumDiscussionService discussions) {
        this.discussions = discussions;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ForumDiscussionService.CreatedDiscussion> createDiscussion(
            @RequestParam("gameId") String gameId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam("cover") MultipartFile cover,
            @RequestParam("tagIds") List<Long> tagIds,
            @AuthenticationPrincipal UserDetails currentUser) {

        ForumDiscussionService.CreatedDiscussion created =
                discussions.create(
                        gameId,
                        title,
                        description,
                        cover,
                        tagIds,
                        currentUser.getUsername()
                );

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ForumDiscussionService.DiscussionPage listDiscussions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return discussions.listPublished(page, size);
    }

    @GetMapping("/{discussionId}/cover")
    public ResponseEntity<byte[]> getCover(@PathVariable Long discussionId) {
        ForumDiscussionService.CoverImage cover =
                discussions.getPublishedCover(discussionId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(cover.contentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(cover.content());
    }

    @GetMapping("/{discussionId}")
    public ForumDiscussionService.DiscussionDetail getDiscussion(
            @PathVariable Long discussionId,
            @AuthenticationPrincipal UserDetails currentUser) {

        String userEmail = currentUser == null
                ? null
                : currentUser.getUsername();

        return discussions.getPublishedDetail(discussionId, userEmail);
    }

    @GetMapping("/{discussionId}/replies")
    public List<ForumDiscussionService.ForumReplyResponse> listReplies(
            @PathVariable Long discussionId) {
        return discussions.listReplies(discussionId);
    }

    @PostMapping("/{discussionId}/replies")
    public ForumDiscussionService.ForumReplyResponse createReply(
            @PathVariable Long discussionId,
            @Valid @RequestBody CreateForumReplyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return discussions.createReply(
                discussionId,
                request,
                userDetails.getUsername()
        );
    }

        @PutMapping("/{discussionId}/rating")
    public ForumDiscussionService.RatingResult rateDiscussion(
            @PathVariable Long discussionId,
            @Valid @RequestBody RateForumDiscussionRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return discussions.rateDiscussion(
                discussionId,
                request.rating(),
                currentUser.getUsername()
        );
    }
}