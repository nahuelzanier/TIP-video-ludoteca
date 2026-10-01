package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.tags.GameTagService;
import com.tip_video_ludoteca.tags.GameTagSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:5173")
public class GameTagController {

    private final GameTagService gameTagService;

    public GameTagController(GameTagService gameTagService) {
        this.gameTagService = gameTagService;
    }

    @GetMapping("/tags")
    public List<GameTagService.TagOption> getAvailableTags() {
        return gameTagService.getAvailableTags();
    }

    @GetMapping("/games/{gameId}/tags")
    public List<GameTagSummary> getGameTags(@PathVariable String gameId) {
        return gameTagService.getGameTags(gameId);
    }

    @GetMapping("/games/{gameId}/tags/mine")
    public List<Long> getMyAssignedTagIds(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails currentUser) {
        return gameTagService.getMyAssignedTagIds(
                gameId,
                currentUser.getUsername()
        );
    }

    @PostMapping("/games/{gameId}/tags")
    public ResponseEntity<List<GameTagSummary>> assignTag(
            @PathVariable String gameId,
            @RequestBody AssignTagRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {

        List<GameTagSummary> updatedTags = gameTagService.assignTag(
                gameId,
                request.tagId(),
                currentUser.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(updatedTags);
    }

    public record AssignTagRequest(Long tagId) {
    }
}