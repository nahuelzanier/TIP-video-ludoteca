package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.GameEditForbiddenException;
import com.tip_video_ludoteca.games.GameNotFoundException;
import com.tip_video_ludoteca.games.GameService;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.games.GameUploadService;
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
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameUploadController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class GamePublishControllerTests {

    private static final String EMAIL = "lucas2@gmail.com";
    private static final String GAME_ID = "game-1";

    @MockitoBean
    private GameUploadService gameUploadService;

    @MockitoBean
    private GameService gameService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishReturnsTheUpdatedStatusForTheAuthor() throws Exception {
        given(gameService.publish(GAME_ID, EMAIL))
                .willReturn(new GameService.PublishResult(
                        GAME_ID, "Mi juego", GameStatus.PUBLISHED));

        mockMvc.perform(post("/api/games/{gameId}/publish", GAME_ID)
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(GAME_ID))
                .andExpect(jsonPath("$.title").value("Mi juego"))
                .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void publishRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/publish", GAME_ID)
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(gameService, never()).publish(GAME_ID, EMAIL);
    }

    @Test
    void publishReturnsForbiddenForAnotherAuthor() throws Exception {
        willThrow(new GameEditForbiddenException())
                .given(gameService).publish(GAME_ID, EMAIL);

        mockMvc.perform(post("/api/games/{gameId}/publish", GAME_ID)
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("No podés publicar el juego de otro usuario."));
    }

    @Test
    void publishReturnsNotFoundForAnUnknownGame() throws Exception {
        willThrow(new GameNotFoundException())
                .given(gameService).publish("noexiste", EMAIL);

        mockMvc.perform(post("/api/games/{gameId}/publish", "noexiste")
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El juego no existe."));
    }
}