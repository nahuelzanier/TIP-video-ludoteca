package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameServiceTests {

    private static final String GAME_ID = "game-1";

    @Mock
    private GameRepository games;

    private GameService gameService;

    @BeforeEach
    void setUp() {
        gameService = new GameService(games);
    }

    @Test
    void publishesADraftGameAfterAdministrativeApproval() {
        Game game = game(GameStatus.DRAFT);
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));

        GameService.PublishResult result = gameService.publishAsAdmin(GAME_ID);

        assertThat(game.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        assertThat(result.id()).isEqualTo(GAME_ID);
        assertThat(result.title()).isEqualTo("Aventura");
        assertThat(result.status()).isEqualTo(GameStatus.PUBLISHED);
        verify(games).save(game);
    }

    @Test
    void failsWhenTheGameDoesNotExist() {
        when(games.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.publishAsAdmin(GAME_ID))
                .isInstanceOf(GameNotFoundException.class);
    }

    private Game game(GameStatus status) {
        User owner = new User("autor", "autor@test.com", "{bcrypt}hash");
        ReflectionTestUtils.setField(owner, "id", 7L);
        return new Game(GAME_ID, owner, "Aventura", "", null, "index.html", status);
    }
}
