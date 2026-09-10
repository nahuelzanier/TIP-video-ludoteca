package com.tip_video_ludoteca.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/games")
public class GameController {

    private final Path gamesPath =
            Paths.get("gamesData").toAbsolutePath().normalize();

    @GetMapping("/{gameId}/**")
    public ResponseEntity<Resource> getGameFile(
            @PathVariable String gameId,
            HttpServletRequest request) {

        try {
            String requestPath = request.getRequestURI();

            String prefix = "/games/" + gameId + "/";
            String filePath = requestPath.substring(prefix.length());

            Path gameDirectory = gamesPath.resolve(gameId).normalize();
            Path targetFile = gameDirectory.resolve(filePath).normalize();

            // Evitar acceder fuera de la carpeta del juego
            if (!targetFile.startsWith(gameDirectory)) {
                return ResponseEntity.badRequest().build();
            }

            Resource resource = new UrlResource(targetFile.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(targetFile);

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

            if (contentType != null) {
                try {
                    mediaType = MediaType.parseMediaType(contentType);
                } catch (Exception ignored) {
                    // Dejamos APPLICATION_OCTET_STREAM
                }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}