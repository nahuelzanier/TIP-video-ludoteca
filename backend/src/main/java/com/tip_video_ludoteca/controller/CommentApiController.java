package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.comments.CommentResponse;
import com.tip_video_ludoteca.comments.CommentService;
import com.tip_video_ludoteca.comments.CreateCommentRequest;
import com.tip_video_ludoteca.comments.CreateReplyRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentApiController {

    private final CommentService comments;

    public CommentApiController(CommentService comments) {
        this.comments = comments;
    }

    @GetMapping("/games/{gameId}/comments")
    public List<CommentResponse> listComments(@PathVariable String gameId) {
        return comments.listByGame(gameId);
    }

    @PostMapping("/games/{gameId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String gameId,
            @Valid @RequestBody CreateCommentRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(comments.create(gameId, request, principal.getUsername()));
    }

    @PostMapping("/comments/{commentId}/replies")
    public ResponseEntity<CommentResponse> createReply(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CreateReplyRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(comments.createReply(commentId, request, principal.getUsername()));
    }
}