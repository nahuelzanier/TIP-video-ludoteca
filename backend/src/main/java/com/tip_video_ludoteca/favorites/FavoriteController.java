package com.tip_video_ludoteca.favorites;

import com.tip_video_ludoteca.controller.GameApiController;
import com.tip_video_ludoteca.games.Game;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favorites;

    public FavoriteController(FavoriteService favorites) {
        this.favorites = favorites;
    }

    @PostMapping("/games/{gameId}")
    public ResponseEntity<FavoriteStatusResponse> addFavorite(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails principal) {
        favorites.addFavorite(gameId, principal.getUsername());
        return ResponseEntity.ok(new FavoriteStatusResponse(gameId, true));
    }

    @DeleteMapping("/games/{gameId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable String gameId,
            @AuthenticationPrincipal UserDetails principal) {
        favorites.removeFavorite(gameId, principal.getUsername());
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public ResponseEntity<FavoritePage> getFavorites(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 9);

        Page<Game> result = favorites.listFavorites(
                principal.getUsername(),
                PageRequest.of(safePage, safeSize));

        List<GameApiController.GameSummary> content = result.getContent().stream()
                .map(game -> new GameApiController.GameSummary(
                        game.getId(),
                        game.getTitle(),
                        game.getCoverImageUrl(),
                        game.getDescription(),
                        true))
                .toList();

        return ResponseEntity.ok(new FavoritePage(
                content,
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.hasNext(),
                result.hasPrevious()));
    }

    public record FavoriteStatusResponse(String gameId, boolean favorite) {
    }

    public record FavoritePage(
            List<GameApiController.GameSummary> content,
            int page,
            int size,
            long totalElements,
            int totalPages,
            boolean hasNext,
            boolean hasPrevious) {
    }
}
