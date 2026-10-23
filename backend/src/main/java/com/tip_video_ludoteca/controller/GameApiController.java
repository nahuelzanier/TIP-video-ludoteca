package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameSpecifications;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.games.NoGamesAvailableException;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/games")
public class GameApiController {

    private final GameRepository games;

    public GameApiController(GameRepository games) {
        this.games = games;
    }

    @GetMapping
    public ResponseEntity<GamePage> getGames(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) List<Long> tagIds,
            @RequestParam(required = false) Double minRating) {

        if (minRating != null
                && (!Double.isFinite(minRating) || minRating < 1 || minRating > 5)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La valoración mínima debe estar entre 1 y 5."
            );
        }

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 24);

        PageRequest pageRequest = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Game> result = games.findAll(
                GameSpecifications.withFilters(q, tagIds, minRating),
                pageRequest
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

    @GetMapping("/random")
    public GameSummary getRandomGame() {
        return games.findRandomByStatus(GameStatus.PUBLISHED.name())
                .map(game -> new GameSummary(
                        game.getId(),
                        game.getTitle(),
                        game.getCoverImageUrl(),
                        game.getDescription()
                ))
                .orElseThrow(NoGamesAvailableException::new);
    }

    @GetMapping("/{gameId}")
    public ResponseEntity<GameSummary> getGame(@PathVariable String gameId) {
        Game game = games.findById(gameId).orElse(null);

        if (game == null || game.getStatus() != GameStatus.PUBLISHED) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(new GameSummary(
                game.getId(),
                game.getTitle(),
                game.getCoverImageUrl(),
                game.getDescription()
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
