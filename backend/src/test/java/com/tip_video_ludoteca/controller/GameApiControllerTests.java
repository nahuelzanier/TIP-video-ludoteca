package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRecommendationSummary;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.tags.GameTagAssignmentRepository;
import com.tip_video_ludoteca.users.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
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

    @MockitoBean
    private GameTagAssignmentRepository gameTagAssignmentRepository;

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

    @Test
    void recommendationsReturnLightweightSummaries() throws Exception {
        given(gameRepository.findById(GAME_ID))
                .willReturn(Optional.of(publishedGame()));
        given(gameTagAssignmentRepository.findRecommendationsForGame(
                GAME_ID, PageRequest.of(0, 3)))
                .willReturn(List.of(
                        new GameRecommendationSummary(
                                "game-2", "Puzzle", "/api/games/game-2/cover"),
                        new GameRecommendationSummary(
                                "game-3", "Carreras", null)
                ));

        mockMvc.perform(get("/api/games/game-1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value("game-2"))
                .andExpect(jsonPath("$[0].title").value("Puzzle"))
                .andExpect(jsonPath("$[0].image")
                        .value("/api/games/game-2/cover"))
                .andExpect(jsonPath("$[1].id").value("game-3"))
                .andExpect(jsonPath("$[1].image").doesNotExist());
    }

    @Test
    void recommendationsReturnEmptyListWhenThereAreNoMatches() throws Exception {
        given(gameRepository.findById(GAME_ID))
                .willReturn(Optional.of(publishedGame()));
        given(gameTagAssignmentRepository.findRecommendationsForGame(
                GAME_ID, PageRequest.of(0, 3)))
                .willReturn(List.of());

        mockMvc.perform(get("/api/games/game-1/recommendations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recommendationsReturnErrorEnvelopeForUnknownGame() throws Exception {
        given(gameRepository.findById("missing"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/games/missing/recommendations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El juego no existe."));
    }

    @Test
    void recommendationsReturnErrorEnvelopeForDraftGame() throws Exception {
        Game draft = new Game(
                GAME_ID,
                author(),
                "Aventura",
                "Un juego de prueba.",
                "/api/games/game-1/cover",
                "index.html",
                GameStatus.DRAFT);
        given(gameRepository.findById(GAME_ID))
                .willReturn(Optional.of(draft));

        mockMvc.perform(get("/api/games/game-1/recommendations"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El juego no existe."));
    }

    private Game publishedGame() {
        return new Game(
                GAME_ID,
                author(),
                "Aventura",
                "Un juego de prueba.",
                "/api/games/game-1/cover",
                "index.html",
                GameStatus.PUBLISHED);
    }

    private User author() {
        User author = new User("autor", "autor@test.com", "{bcrypt}hash");
        ReflectionTestUtils.setField(author, "id", 7L);
        return author;
    }
}
