package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.followers.FollowService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/follows")
public class FollowApiController {

    private final FollowService follows;

    public FollowApiController(FollowService follows) {
        this.follows = follows;
    }

    @PutMapping("/{username}")
    public FollowStatusResponse follow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails principal) {
        follows.follow(username, principal.getUsername());
        return new FollowStatusResponse(username, true);
    }

    @DeleteMapping("/{username}")
    public ResponseEntity<Void> unfollow(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails principal) {
        follows.unfollow(username, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/status")
    public FollowStatusResponse status(
            @PathVariable String username,
            @AuthenticationPrincipal UserDetails principal) {
        return new FollowStatusResponse(
                username,
                follows.isFollowing(username, principal.getUsername()));
    }

    @GetMapping("/{username}/counts")
    public FollowService.FollowCounts counts(@PathVariable String username) {
        return follows.counts(username);
    }

    public record FollowStatusResponse(String username, boolean following) {
    }
}
