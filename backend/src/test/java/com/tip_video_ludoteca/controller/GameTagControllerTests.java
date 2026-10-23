package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.tags.GameTagService;
import com.tip_video_ludoteca.tags.GameTagSummary;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameTagController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class GameTagControllerTests {

    private static final String GAME_ID = "game-1";
    private static final String EMAIL = "player@ludarium.test";

    @MockitoBean
    private GameTagService gameTagService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listsAvailableAndGameTagsPublicly() throws Exception {
        given(gameTagService.getAvailableTags()).willReturn(List.of(
                new GameTagService.TagOption(2L, "Puzzle", "puzzle")));
        given(gameTagService.getGameTags(GAME_ID)).willReturn(List.of(
                new GameTagSummary(2L, "Puzzle", "puzzle", 3L,
                        java.time.Instant.parse("2026-10-16T00:00:00Z"))));

        mockMvc.perform(get("/api/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Puzzle"));
        mockMvc.perform(get("/api/games/{gameId}/tags", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].assignmentCount").value(3));
    }

    @Test
    void myTagsRequiresLogin() throws Exception {
        mockMvc.perform(get("/api/games/{gameId}/tags/mine", GAME_ID))
                .andExpect(status().isUnauthorized());

        verify(gameTagService, never()).getMyAssignedTagIds(GAME_ID, EMAIL);
    }

    @Test
    void authenticatedUserCanReadTheirTagAssignments() throws Exception {
        given(gameTagService.getMyAssignedTagIds(GAME_ID, EMAIL)).willReturn(List.of(2L, 5L));

        mockMvc.perform(get("/api/games/{gameId}/tags/mine", GAME_ID).with(user(EMAIL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value(2))
                .andExpect(jsonPath("$[1]").value(5));
    }

    @Test
    void assigningTagRequiresLoginAndCsrf() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/tags", GAME_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagId\":2}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/games/{gameId}/tags", GAME_ID)
                        .with(user(EMAIL))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagId\":2}"))
                .andExpect(status().isForbidden());

        verify(gameTagService, never()).assignTag(GAME_ID, 2L, EMAIL);
    }

    @Test
    void authenticatedUserCanAssignTagWithCsrf() throws Exception {
        given(gameTagService.assignTag(GAME_ID, 2L, EMAIL)).willReturn(List.of(
                new GameTagSummary(2L, "Puzzle", "puzzle", 1L,
                        java.time.Instant.parse("2026-10-16T00:00:00Z"))));

        mockMvc.perform(post("/api/games/{gameId}/tags", GAME_ID)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tagId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Puzzle"));
    }
}
