package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class GameApiControllerTests {

    private static final String GAME_ID = "game-1";

    @MockitoBean
    private GameRepository gameRepository;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void randomReturnsAPublishedGame() throws Exception {
        given(gameRepository.findRandomByStatus("PUBLISHED"))
                .willReturn(Optional.of(publishedGame()));

        mockMvc.perform(get("/api/games/random"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(GAME_ID))
                .andExpect(jsonPath("$.title").value("Aventura"))
                .andExpect(jsonPath("$.image").value("/api/games/game-1/cover"))
                .andExpect(jsonPath("$.description").value("Un juego de prueba."));

        verify(gameRepository).findRandomByStatus("PUBLISHED");
    }

    @Test
    void randomReturnsNotFoundWhenThereAreNoGames() throws Exception {
        given(gameRepository.findRandomByStatus("PUBLISHED"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/games/random"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error")
                        .value("There are no games available yet."));
    }

    private Game publishedGame() {
        User author = new User("autor", "autor@test.com", "{bcrypt}hash");
        ReflectionTestUtils.setField(author, "id", 7L);

        return new Game(
                GAME_ID,
                author,
                "Aventura",
                "Un juego de prueba.",
                "/api/games/game-1/cover",
                "index.html",
                GameStatus.PUBLISHED);
    }
}
