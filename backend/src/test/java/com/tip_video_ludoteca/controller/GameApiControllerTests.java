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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
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
                        new GameRecommendationSummary("game-3", "Carreras", null)
                ));

        mockMvc.perform(get("/api/games/{gameId}/recommendations", GAME_ID))
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

        mockMvc.perform(get("/api/games/{gameId}/recommendations", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void recommendationsReturnErrorEnvelopeForUnknownGame() throws Exception {
        given(gameRepository.findById("missing"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/games/{gameId}/recommendations", "missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El juego no existe."));
    }

    @Test
    void recommendationsReturnErrorEnvelopeForDraftGame() throws Exception {
        given(gameRepository.findById(GAME_ID))
                .willReturn(Optional.of(game(GameStatus.DRAFT)));

        mockMvc.perform(get("/api/games/{gameId}/recommendations", GAME_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El juego no existe."));
    }

    @Test
    void listsPublishedGamesWithDefaultPagination() throws Exception {
        given(gameRepository.findAll(anySpecification(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(publishedGame()), pageRequest(0, 12), 1));

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(GAME_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(gameRepository).findAll(anySpecification(), eq(pageRequest(0, 12)));
    }

    @Test
    void passesSearchAndRepeatedGenreFiltersToPagedRepositoryQuery() throws Exception {
        given(gameRepository.findAll(anySpecification(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(publishedGame()), pageRequest(0, 12), 1));

        mockMvc.perform(get("/api/games")
                        .param("q", "  puzzle ")
                        .param("tagIds", "2", "5")
                        .param("minRating", "4.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(GAME_ID));

        verify(gameRepository).findAll(anySpecification(), eq(pageRequest(0, 12)));
    }

    @Test
    void clampsPageAndSize() throws Exception {
        given(gameRepository.findAll(anySpecification(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), pageRequest(0, 24), 0));

        mockMvc.perform(get("/api/games")
                        .param("page", "-4")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(24));

        verify(gameRepository).findAll(anySpecification(), eq(pageRequest(0, 24)));
    }

    @Test
    void rejectsMinimumRatingOutsideTheOneToFiveRange() throws Exception {
        mockMvc.perform(get("/api/games").param("minRating", "6"))
                .andExpect(status().isBadRequest());

        verify(gameRepository, never()).findAll(anySpecification(), any(Pageable.class));
    }

    @Test
    void returnsPublishedGameByIdButHidesDraftAndUnknownGames() throws Exception {
        given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(publishedGame()));
        given(gameRepository.findById("draft"))
                .willReturn(Optional.of(game(GameStatus.DRAFT)));
        given(gameRepository.findById("missing")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/games/{gameId}", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Aventura"));
        mockMvc.perform(get("/api/games/{gameId}", "draft"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/games/{gameId}", "missing"))
                .andExpect(status().isNotFound());
    }

    @SuppressWarnings("unchecked")
    private Specification<Game> anySpecification() {
        return org.mockito.ArgumentMatchers.any(Specification.class);
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
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

    private Game game(GameStatus status) {
        return new Game(
                "draft",
                author(),
                "Borrador",
                "",
                null,
                "index.html",
                status);
    }

    private User author() {
        User author = new User("autor", "autor@test.com", "{bcrypt}hash");
        ReflectionTestUtils.setField(author, "id", 7L);
        return author;
    }
}