package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.GameCustomizationService;
import com.tip_video_ludoteca.games.UpdateGameCustomizationRequest;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameCustomizationController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class GameCustomizationControllerTests {

    private static final String GAME_ID = "game-1";

    @MockitoBean
    private GameCustomizationService customizations;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void anyoneCanReadThePublishedGameAppearance() throws Exception {
        given(customizations.get(GAME_ID)).willReturn(
                new GameCustomizationService.Customization(
                        "#17151f", "#a36aff", "#f5f4f8", "SYSTEM", "SOLID"));

        mockMvc.perform(get("/api/games/{gameId}/customization", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.backgroundColor").value("#17151f"))
                .andExpect(jsonPath("$.accentColor").value("#a36aff"))
                .andExpect(jsonPath("$.textColor").value("#f5f4f8"))
                .andExpect(jsonPath("$.fontFamily").value("SYSTEM"))
                .andExpect(jsonPath("$.backgroundStyle").value("SOLID"));
    }

    @Test
    void savingRequiresAnAuthenticatedUserAndCsrfToken() throws Exception {
        String body = validBody();

        mockMvc.perform(put("/api/games/{gameId}/customization", GAME_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/games/{gameId}/customization", GAME_ID)
                        .with(user("player@example.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void authenticatedOwnerCanSaveAValidCustomization() throws Exception {
        given(customizations.update(GAME_ID, "creator@example.test",
                new UpdateGameCustomizationRequest(
                        "#102030", "#d080ff", "#eeeeee", "GEORGIA", "GRADIENT")))
                .willReturn(new GameCustomizationService.Customization(
                        "#102030", "#d080ff", "#eeeeee", "GEORGIA", "GRADIENT"));

        mockMvc.perform(put("/api/games/{gameId}/customization", GAME_ID)
                        .with(user("creator@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fontFamily").value("GEORGIA"))
                .andExpect(jsonPath("$.backgroundStyle").value("GRADIENT"));

        verify(customizations).update(GAME_ID, "creator@example.test",
                new UpdateGameCustomizationRequest(
                        "#102030", "#d080ff", "#eeeeee", "GEORGIA", "GRADIENT"));
    }

    @Test
    void invalidColorsAndFontsAreRejected() throws Exception {
        mockMvc.perform(put("/api/games/{gameId}/customization", GAME_ID)
                        .with(user("creator@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"backgroundColor\":\"red\",\"accentColor\":\"#d080ff\","
                                + "\"textColor\":\"#eeeeee\",\"fontFamily\":\"ComicSans\","
                                + "\"backgroundStyle\":\"GRADIENT\"}"))
                .andExpect(status().isBadRequest());
    }

    private String validBody() {
        return "{\"backgroundColor\":\"#102030\",\"accentColor\":\"#d080ff\","
                + "\"textColor\":\"#eeeeee\",\"fontFamily\":\"GEORGIA\","
                + "\"backgroundStyle\":\"GRADIENT\"}";
    }
}
