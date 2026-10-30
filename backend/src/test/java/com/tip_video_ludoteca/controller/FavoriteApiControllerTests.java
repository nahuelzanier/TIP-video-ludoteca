package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.favorites.FavoriteController;
import com.tip_video_ludoteca.favorites.FavoriteGame;
import com.tip_video_ludoteca.favorites.FavoriteService;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameStatus;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FavoriteController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class FavoriteApiControllerTests {

    private static final String EMAIL = "user@test.com";
    private static final String GAME_ID = "game-1";

    @MockitoBean
    private FavoriteService favoriteService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void addsFavoriteForAuthenticatedUsers() throws Exception {
        given(favoriteService.addFavorite(GAME_ID, EMAIL))
                .willReturn(new FavoriteGame(testUser(1L, "user"), game()));

        mockMvc.perform(post("/api/favorites/games/{gameId}", GAME_ID)
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId").value(GAME_ID))
                .andExpect(jsonPath("$.favorite").value(true));
    }

    @Test
    void rejectsTheRequestWhenUserIsNotAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/favorites/games/{gameId}", GAME_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsFavoritesPageForTheAuthenticatedUser() throws Exception {
        given(favoriteService.listFavorites(EMAIL, org.springframework.data.domain.PageRequest.of(0, 9)))
                .willReturn(new PageImpl<>(List.of(game()), org.springframework.data.domain.PageRequest.of(0, 9), 1L));

        mockMvc.perform(get("/api/favorites")
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .param("page", "0")
                        .param("size", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(GAME_ID))
                .andExpect(jsonPath("$.content[0].favorito").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    private User testUser(long id, String username) {
        User user = new User(username, EMAIL, "{bcrypt}hash");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Game game() {
        return new Game(GAME_ID, testUser(1L, "user"), "Aventura", "", null, "index.html", GameStatus.PUBLISHED);
    }
}
