package com.tip_video_ludoteca.favorites;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTests {

    private static final String EMAIL = "user@test.com";
    private static final String GAME_ID = "game-1";

    @Mock
    private FavoriteGameRepository favoriteGames;

    @Mock
    private GameRepository games;

    @Mock
    private UserRepository users;

    private FavoriteService favoriteService;

    @BeforeEach
    void setUp() {
        favoriteService = new FavoriteService(favoriteGames, games, users);
    }

    @Test
    void addsFavoriteWhenItDoesNotExist() {
        User user = user(1L, "user");
        Game game = game(user, GameStatus.PUBLISHED);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(favoriteGames.findByUser_IdAndGame_Id(1L, GAME_ID)).thenReturn(Optional.empty());
        when(favoriteGames.save(any(FavoriteGame.class))).thenReturn(new FavoriteGame(user, game));

        FavoriteGame result = favoriteService.addFavorite(GAME_ID, EMAIL);

        assertThat(result.getGame().getId()).isEqualTo(GAME_ID);
        assertThat(result.getUser().getEmail()).isEqualTo(EMAIL);
        verify(favoriteGames).save(any(FavoriteGame.class));
    }

    @Test
    void keepsFavoriteIdempotentWhenItAlreadyExists() {
        User user = user(2L, "user2");
        Game game = game(user, GameStatus.PUBLISHED);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(favoriteGames.findByUser_IdAndGame_Id(2L, GAME_ID))
                .thenReturn(Optional.of(new FavoriteGame(user, game)));

        FavoriteGame result = favoriteService.addFavorite(GAME_ID, EMAIL);

        assertThat(result.getId().getGameId()).isEqualTo(GAME_ID);
        assertThat(result.getId().getUserId()).isEqualTo(2L);
    }

    @Test
    void removesFavoriteWhenRequested() {
        User user = user(3L, "user3");
        Game game = game(user, GameStatus.PUBLISHED);

        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(favoriteGames.findByUser_IdAndGame_Id(3L, GAME_ID))
                .thenReturn(Optional.of(new FavoriteGame(user, game)));

        favoriteService.removeFavorite(GAME_ID, EMAIL);

        verify(favoriteGames).delete(any(FavoriteGame.class));
    }

    @Test
    void listsPublishedFavoritesForTheAuthenticatedUser() {
        User user = user(4L, "user4");
        Game game = game(user, GameStatus.PUBLISHED);

        when(favoriteGames.findGamesByUserEmailAndStatus(
                EMAIL,
                GameStatus.PUBLISHED,
                PageRequest.of(0, 9)))
                .thenReturn(new PageImpl<>(List.of(game), PageRequest.of(0, 9), 1L));

        var result = favoriteService.listFavorites(EMAIL, PageRequest.of(0, 9));

        assertThat(result.getContent())
                .extracting(Game::getId)
                .containsExactly(GAME_ID);
    }

    private User user(long id, String username) {
        User user = new User(username, EMAIL, "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Game game(User owner, GameStatus status) {
        return new Game(GAME_ID, owner, "Aventura", "", null, "index.html", status);
    }
}
