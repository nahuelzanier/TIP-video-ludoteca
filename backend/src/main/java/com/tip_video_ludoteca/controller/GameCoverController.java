package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameFile;
import com.tip_video_ludoteca.games.GameFileRepository;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

@RestController
@RequestMapping("/api/games")
public class GameCoverController {

    private static final String COVER_FILE_PATH = "__metadata__/thumbnail";

    private final GameFileRepository gameFiles;
    private final GameRepository games;

    public GameCoverController(GameFileRepository gameFiles, GameRepository games) {
        this.gameFiles = gameFiles;
        this.games = games;
    }

    @GetMapping("/{gameId}/cover")
    public ResponseEntity<byte[]> getCover(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails principal) {
        boolean isAdmin = principal != null && principal.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        boolean isPublished = games.findById(gameId)
                .map(game -> game.getStatus() == GameStatus.PUBLISHED)
                .orElse(false);
        if (!isPublished && !isAdmin) return ResponseEntity.notFound().build();
        return gameFiles.findById_GameIdAndId_Path(gameId, COVER_FILE_PATH)
                .map(this::toResponse)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private ResponseEntity<byte[]> toResponse(GameFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(file.getContent());
    }
}
