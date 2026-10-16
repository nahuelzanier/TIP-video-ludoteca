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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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

    @Test
    void listsPublishedGamesWithDefaultPagination() throws Exception {
        given(gameRepository.findByStatus(
                org.mockito.ArgumentMatchers.eq(GameStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<>(java.util.List.of(publishedGame()),
                        PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/api/games"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(GAME_ID))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(gameRepository).findByStatus(GameStatus.PUBLISHED, PageRequest.of(
                0, 12, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @Test
    void clampsPageAndSizeAndTrimsSearchQuery() throws Exception {
        given(gameRepository.findByStatusAndTitleContainingIgnoreCase(
                org.mockito.ArgumentMatchers.eq(GameStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.eq("puzzle"),
                org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 24), 0));

        mockMvc.perform(get("/api/games")
                        .param("page", "-4")
                        .param("size", "100")
                        .param("q", "  puzzle  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(24));

        verify(gameRepository).findByStatusAndTitleContainingIgnoreCase(
                GameStatus.PUBLISHED,
                "puzzle",
                PageRequest.of(0, 24, Sort.by(Sort.Direction.DESC, "createdAt")));
        verify(gameRepository, never()).findByStatus(org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void blankSearchUsesUnfilteredPublishedList() throws Exception {
        given(gameRepository.findByStatus(
                org.mockito.ArgumentMatchers.eq(GameStatus.PUBLISHED),
                org.mockito.ArgumentMatchers.any()))
                .willReturn(new PageImpl<>(java.util.List.of(), PageRequest.of(0, 12), 0));

        mockMvc.perform(get("/api/games").param("q", "   "))
                .andExpect(status().isOk());

        verify(gameRepository).findByStatus(GameStatus.PUBLISHED, PageRequest.of(
                0, 12, Sort.by(Sort.Direction.DESC, "createdAt")));
        verify(gameRepository, never()).findByStatusAndTitleContainingIgnoreCase(
                org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void returnsPublishedGameByIdButHidesDraftAndUnknownGames() throws Exception {
        given(gameRepository.findById(GAME_ID)).willReturn(Optional.of(publishedGame()));
        given(gameRepository.findById("draft")).willReturn(Optional.of(game(GameStatus.DRAFT)));
        given(gameRepository.findById("missing")).willReturn(Optional.empty());

        mockMvc.perform(get("/api/games/{gameId}", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Aventura"));
        mockMvc.perform(get("/api/games/{gameId}", "draft"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/games/{gameId}", "missing"))
                .andExpect(status().isNotFound());
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

    private Game game(GameStatus status) {
        User author = new User("autor", "autor@test.com", "{bcrypt}hash");
        return new Game("draft", author, "Borrador", "", null, "index.html", status);
    }
}
