package com.tip_video_ludoteca.search;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@RestController
@RequestMapping("/api/search")
public class SearchController {

    private static final int MIN_TERM_LENGTH = 2;
    private static final int MAX_TERM_LENGTH = 100;
    private static final String DEFAULT_SIZE = "9";
    private static final int MAX_SIZE = 50;

    private final GameRepository games;
    private final UserRepository users;

    public SearchController(GameRepository games, UserRepository users) {
        this.games = games;
        this.users = users;
    }

    @GetMapping
    public ResponseEntity<?> search(
            @RequestParam(name = "q", required = false) String query,
            @RequestParam(name = "gamesPage", required = false, defaultValue = "0") int gamesPage,
            @RequestParam(name = "usersPage", required = false, defaultValue = "0") int usersPage,
            @RequestParam(name = "size", required = false, defaultValue = DEFAULT_SIZE) int size) {

        String validationError = validate(query, gamesPage, usersPage, size);
        if (validationError != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", validationError));
        }

        String term = query.trim();

        PageResult<GameItem> gameResults = toPage(
                games.findByTitleContainingIgnoreCase(term, PageRequest.of(gamesPage, size)),
                GameItem::from);

        PageResult<UserItem> userResults = toPage(
                users.findByUsernameContainingIgnoreCase(term, PageRequest.of(usersPage, size)),
                UserItem::from);

        return ResponseEntity.ok(new SearchResponse(term, gameResults, userResults));
    }

    private String validate(String query, int gamesPage, int usersPage, int size) {
        if (query == null || query.isBlank()) {
            return "Escribí un término para buscar.";
        }

        String term = query.trim();

        if (term.length() < MIN_TERM_LENGTH || term.length() > MAX_TERM_LENGTH) {
            return "El término debe tener entre " + MIN_TERM_LENGTH + " y " + MAX_TERM_LENGTH + " caracteres.";
        }

        if (gamesPage < 0 || usersPage < 0) {
            return "El número de página no puede ser negativo.";
        }

        if (size < 1 || size > MAX_SIZE) {
            return "El tamaño de página debe estar entre 1 y " + MAX_SIZE + ".";
        }

        return null;
    }

    private <S, T> PageResult<T> toPage(Page<S> page, Function<S, T> mapper) {
        List<T> items = page.getContent().stream().map(mapper).toList();

        return new PageResult<>(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    public record GameItem(
            String id,
            String title,
            String image,
            String description) {

        static GameItem from(Game game) {
            return new GameItem(
                    game.getId(),
                    game.getTitle(),
                    game.getCoverImageUrl(),
                    game.getDescription());
        }
    }

    public record UserItem(
            Long id,
            String username) {

        static UserItem from(User user) {
            return new UserItem(user.getId(), user.getUsername());
        }
    }

    public record PageResult<T>(
            List<T> items,
            int page,
            int size,
            long totalItems,
            int totalPages) {
    }

    public record SearchResponse(
            String query,
            PageResult<GameItem> games,
            PageResult<UserItem> users) {
    }
}
