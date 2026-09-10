package com.tip_video_ludoteca.backend.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:5173")
public class GameApiController {

    @GetMapping
    public ResponseEntity<String> getGames() {
        try {
            ClassPathResource resource = new ClassPathResource("games.json");

            String json = new String(
                    resource.getInputStream().readAllBytes(),
                    StandardCharsets.UTF_8
            );

            return ResponseEntity.ok(json);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}