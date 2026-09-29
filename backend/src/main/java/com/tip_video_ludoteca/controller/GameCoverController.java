package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameFile;
import com.tip_video_ludoteca.games.GameFileRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:5173")
public class GameCoverController {

    private static final String COVER_FILE_PATH = "__metadata__/thumbnail";

    private final GameFileRepository gameFiles;

    public GameCoverController(GameFileRepository gameFiles) {
        this.gameFiles = gameFiles;
    }

    @GetMapping("/{gameId}/cover")
    public ResponseEntity<byte[]> getCover(@PathVariable String gameId) {
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