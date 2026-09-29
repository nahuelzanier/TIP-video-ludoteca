package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameFile;
import com.tip_video_ludoteca.games.GameFileRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/games")
public class GameController {

    private final GameFileRepository gameFiles;

    public GameController(GameFileRepository gameFiles) {
        this.gameFiles = gameFiles;
    }

    @GetMapping("/{gameId}/**")
    public ResponseEntity<byte[]> getGameFile(
            @PathVariable String gameId,
            HttpServletRequest request) {
        try {
            String requestPath = UriUtils.decode(
                    request.getRequestURI(),
                    StandardCharsets.UTF_8
            );

            String prefix = request.getContextPath() + "/games/" + gameId + "/";
            if (!requestPath.startsWith(prefix)) {
                return ResponseEntity.badRequest().build();
            }

            String filePath = requestPath.substring(prefix.length());
            Path normalized = Paths.get(filePath).normalize();

            if (filePath.isBlank()
                    || normalized.isAbsolute()
                    || normalized.startsWith("..")) {
                return ResponseEntity.badRequest().build();
            }

            String databasePath = normalized.toString().replace("\\", "/");

            return gameFiles.findById_GameIdAndId_Path(gameId, databasePath)
                    .map(this::toResponse)
                    .orElseGet(() -> ResponseEntity.notFound().build());

        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().build();
        }
    }

    private ResponseEntity<byte[]> toResponse(GameFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .body(file.getContent());
    }
}