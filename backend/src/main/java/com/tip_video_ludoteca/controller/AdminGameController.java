package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameService;
import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/games")
public class AdminGameController {

    private final GameRepository games;
    private final GameService gameService;

    public AdminGameController(GameRepository games, GameService gameService) {
        this.games = games;
        this.gameService = gameService;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ReviewGame> drafts() {
        return games.findByStatus(GameStatus.DRAFT, Sort.by(Sort.Direction.ASC, "createdAt"))
                .stream()
                .map(game -> new ReviewGame(
                        game.getId(), game.getTitle(), game.getDescription(),
                        game.getCoverImageUrl(), game.getOwner().getUsername(),
                        game.getCreatedAt()))
                .toList();
    }

    @PostMapping("/{gameId}/publish")
    public GameService.PublishResult publish(@PathVariable String gameId) {
        return gameService.publishAsAdmin(gameId);
    }

    public record ReviewGame(
            String id, String title, String description, String image,
            String ownerUsername, Instant createdAt) { }
}
