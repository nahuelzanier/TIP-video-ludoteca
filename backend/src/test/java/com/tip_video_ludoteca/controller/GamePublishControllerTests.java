package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameService;
import com.tip_video_ludoteca.games.GameStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminGameController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class GamePublishControllerTests {

    private static final String GAME_ID = "game-1";

    @MockitoBean
    private GameRepository games;

    @MockitoBean
    private GameService gameService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anAdministratorCanPublishAGame() throws Exception {
        given(gameService.publishAsAdmin(GAME_ID))
                .willReturn(new GameService.PublishResult(
                        GAME_ID, "Mi juego", GameStatus.PUBLISHED));

        mockMvc.perform(post("/api/admin/games/{gameId}/publish", GAME_ID)
                        .with(user("admin@test.com").roles("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void aRegularUserCannotPublishAGame() throws Exception {
        mockMvc.perform(post("/api/admin/games/{gameId}/publish", GAME_ID)
                        .with(user("author@test.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(gameService, never()).publishAsAdmin(GAME_ID);
    }

    @Test
    void theFormerAuthorPublishRouteIsNotAvailable() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/publish", GAME_ID)
                        .with(user("author@test.com").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNotFound());
    }
}
