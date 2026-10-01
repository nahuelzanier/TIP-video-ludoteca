package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameService;
import com.tip_video_ludoteca.games.GameUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/games")
public class GameUploadController {

    private final GameUploadService gameUploadService;
    private final GameService gameService;

    public GameUploadController(
            GameUploadService gameUploadService,
            GameService gameService) {
        this.gameUploadService = gameUploadService;
        this.gameService = gameService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GameUploadService.UploadResult> upload(
            @RequestParam("title") String title,
            @RequestParam(value = "description", defaultValue = "") String description,
            @RequestParam("cover") MultipartFile cover,
            @RequestParam("archive") MultipartFile archive,
            @AuthenticationPrincipal UserDetails currentUser) {

        GameUploadService.UploadResult result = gameUploadService.upload(
                title,
                description,
                cover,
                archive,
                currentUser.getUsername()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/{gameId}/publish")
    public GameService.PublishResult publish(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails currentUser) {

        return gameService.publish(gameId, currentUser.getUsername());
    }
}