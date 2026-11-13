package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameFile;
import com.tip_video_ludoteca.games.GameFileRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameCoverController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class GameCoverControllerTests {

    private static final String GAME_ID = "game-1";
    private static final byte[] COVER = {1, 2, 3};

    @MockitoBean
    private GameFileRepository gameFiles;

    @MockitoBean
    private GameRepository games;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsCoverWithStoredTypeAndNosniffHeader() throws Exception {
        given(games.findById(GAME_ID)).willReturn(Optional.of(publishedGame()));
        given(gameFiles.findById_GameIdAndId_Path(GAME_ID, "__metadata__/thumbnail"))
                .willReturn(Optional.of(cover("image/png")));

        mockMvc.perform(get("/api/games/{gameId}/cover", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(COVER));
    }

    @Test
    void returnsNotFoundWhenTheGameHasNoCover() throws Exception {
        given(games.findById(GAME_ID)).willReturn(Optional.of(publishedGame()));
        given(gameFiles.findById_GameIdAndId_Path(GAME_ID, "__metadata__/thumbnail"))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/api/games/{gameId}/cover", GAME_ID))
                .andExpect(status().isNotFound());
    }

    private GameFile cover(String contentType) {
        return new GameFile(publishedGame(), "__metadata__/thumbnail", contentType, COVER);
    }

    private Game publishedGame() {
        User owner = new User("owner", "owner@ludarium.test", "{noop}encoded");
        Game game = new Game(GAME_ID, owner, "Game", "", null,
                "index.html", GameStatus.PUBLISHED);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        return game;
    }
}
