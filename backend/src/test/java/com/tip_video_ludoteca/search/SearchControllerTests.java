package com.tip_video_ludoteca.search;

import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerTests {

    @MockitoBean
    private GameRepository games;

    @MockitoBean
    private UserRepository users;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void trimsTheSearchTermAndReturnsPagedGamesAndUsers() throws Exception {
        given(games.findByTitleContainingIgnoreCase(any(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));
        given(users.findByUsernameContainingIgnoreCase(any(), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 9), 0));

        mockMvc.perform(get("/api/search")
                        .param("q", "  game  "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("game"))
                .andExpect(jsonPath("$.games.items").isArray())
                .andExpect(jsonPath("$.users.items").isArray());

        verify(games).findByTitleContainingIgnoreCase("game", PageRequest.of(0, 9));
        verify(users).findByUsernameContainingIgnoreCase("game", PageRequest.of(0, 9));
    }

    @Test
    void rejectsMissingAndTooShortTerms() throws Exception {
        mockMvc.perform(get("/api/search"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Escribí un término para buscar."));

        mockMvc.perform(get("/api/search").param("q", "x"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "El término debe tener entre 2 y 100 caracteres."));
    }

    @Test
    void rejectsOversizedTermsNegativePagesAndInvalidPageSize() throws Exception {
        mockMvc.perform(get("/api/search").param("q", "a".repeat(101)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/search").param("q", "game").param("gamesPage", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "El número de página no puede ser negativo."));

        mockMvc.perform(get("/api/search").param("q", "game").param("usersPage", "-1"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/search").param("q", "game").param("size", "51"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "El tamaño de página debe estar entre 1 y 50."));

        mockMvc.perform(get("/api/search").param("q", "game").param("size", "0"))
                .andExpect(status().isBadRequest());
    }
}
