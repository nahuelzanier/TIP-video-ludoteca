package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

    private static final String GAME_ID = "game-1";
    private static final String EMAIL = "autor@test.com";
    private static final long OWNER_ID = 7L;

    @Mock
    private GameRepository games;

    @Mock
    private UserRepository users;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(games, users);
    }

    @Test
    void publishesADraftGameWhenTheRequesterIsTheAuthor() {
        User author = user(OWNER_ID, "autor");
        Game game = game(author, GameStatus.DRAFT);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));

        GameService.PublishResult result = gameService.publish(GAME_ID, EMAIL);

        assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        assertThat(result.id()).isEqualTo(GAME_ID);
        assertThat(result.title()).isEqualTo("Aventura");
        assertThat(result.status()).isEqualTo(GameStatus.PUBLISHED);
        verify(games).save(game);
    }

    @Test
    void keepsTheGamePublishedWhenItWasAlreadyPublished() {
        User author = user(OWNER_ID, "autor");
        Game game = game(author, GameStatus.PUBLISHED);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(author));

        GameService.PublishResult result = gameService.publish(GAME_ID, EMAIL);

        assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        assertThat(result.status()).isEqualTo(GameStatus.PUBLISHED);
        verify(games).save(game);
    }

    @Test
    void rejectsPublishingAGameOwnedBySomebodyElse() {
        User author = user(OWNER_ID, "autor");
        User otherUser = user(99L, "otro");
        Game game = game(author, GameStatus.DRAFT);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> gameService.publish(GAME_ID, EMAIL))
                .isInstanceOf(GameEditForbiddenException.class);

        assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFT);
        verify(games, never()).save(any(Game.class));
    }

    @Test
    void failsWhenTheGameDoesNotExist() {
        when(games.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.publish(GAME_ID, EMAIL))
                .isInstanceOf(GameNotFoundException.class);

        verify(users, never()).findByEmailIgnoreCase(any());
        verify(games, never()).save(any(Game.class));
    }

    @Test
    void failsWhenTheAuthenticatedUserDoesNotExist() {
        User author = user(OWNER_ID, "autor");
        Game game = game(author, GameStatus.DRAFT);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.publish(GAME_ID, EMAIL))
                .isInstanceOf(UserNotFoundException.class);

        assertThat(game.getStatus()).isEqualTo(GameStatus.DRAFT);
        verify(games, never()).save(any(Game.class));
    }

    private User user(long id, String username) {
        User user = new User(username, username + "@test.com", "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Game game(User owner, GameStatus status) {
        return new Game(GAME_ID, owner, "Aventura", "", null, "index.html", status);
    }
}