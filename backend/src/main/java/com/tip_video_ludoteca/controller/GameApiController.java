package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
    public ResponseEntity<GamePage> getGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 24);

        Page<Game> result = games.findByStatus(
                GameStatus.PUBLISHED,
                PageRequest.of(
                        safePage,
                        safeSize,
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
        );

        List<GameSummary> content = result.getContent().stream()
                .map(game -> new GameSummary(
                        game.getId(),
                        game.getTitle(),
                        game.getCoverImageUrl(),
                        game.getDescription()
                ))
                .toList();

        return ResponseEntity.ok(new GamePage(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()
        ));
    }

    public record GameSummary(
            String id,
            String title,
            String image,
            String description
    ) {
    }

    public record GamePage(
            List<GameSummary> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious
    ) {
    }
}