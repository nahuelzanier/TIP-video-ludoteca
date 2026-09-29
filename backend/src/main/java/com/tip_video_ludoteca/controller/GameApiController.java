package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/games")
@CrossOrigin(origins = "http://localhost:5173")
public class GameApiController {

    private final GameRepository games;

    public GameApiController(GameRepository games) {
        this.games = games;
    }

    @GetMapping
    public ResponseEntity<List<GameSummary>> getGames() {
        List<GameSummary> result = games
                .findByStatusOrderByCreatedAtDesc(GameStatus.PUBLISHED)
                .stream()
                .map(game -> new GameSummary(
                        game.getId(),
                        game.getTitle(),
                        game.getCoverImageUrl(),
                        game.getDescription()
                ))
                .toList();

        return ResponseEntity.ok(result);
    }

    public record GameSummary(
            String id,
            String title,
            String image,
            String description
    ) {
    }
}