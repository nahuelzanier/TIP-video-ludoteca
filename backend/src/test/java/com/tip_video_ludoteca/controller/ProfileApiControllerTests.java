package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.users.ProfileEditForbiddenException;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserProfileImageService;
import com.tip_video_ludoteca.users.UserProfileResponse;
import com.tip_video_ludoteca.users.UserProfileService;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProfileApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class ProfileApiControllerTests {

    private static final String EMAIL = "luqui@ludarium.test";
    private static final String USERNAME = "luqui";
    private static final String DESCRIPTION = "Diseño juegos desde 2019.";
    private static final String AVATAR_URL = "/api/users/luqui/avatar";

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02
    };

    @MockitoBean
    private UserProfileService userProfiles;

    @MockitoBean
    private UserProfileImageService profileImages;

    @MockitoBean
    private GameRepository games;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProfileIsPublic() throws Exception {
        given(userProfiles.getProfile(USERNAME))
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION, null));

        mockMvc.perform(get("/api/users/{username}/profile", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.avatarUrl").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void getProfileExposesTheAvatarUrl() throws Exception {
        given(userProfiles.getProfile(USERNAME))
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION, AVATAR_URL));

        mockMvc.perform(get("/api/users/{username}/profile", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(AVATAR_URL));
    }

    @Test
    void getProfileReturnsNotFoundForUnknownUser() throws Exception {
        given(userProfiles.getProfile("noexiste"))
                .willThrow(new UserNotFoundException());

        mockMvc.perform(get("/api/users/{username}/profile", "noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Usuario no encontrado."));
    }

    @Test
    void updateDescriptionAllowsTheOwner() throws Exception {
        given(userProfiles.updateDescription(EMAIL, USERNAME, "  " + DESCRIPTION + "  "))
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION, null));

        mockMvc.perform(patch("/api/users/{username}/description", USERNAME)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"  " + DESCRIPTION + "  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value(DESCRIPTION));
    }

    @Test
    void updateDescriptionRejectsAnotherUserProfile() throws Exception {
        willThrow(new ProfileEditForbiddenException())
                .given(userProfiles).updateDescription(EMAIL, "otro", "hack");

        mockMvc.perform(patch("/api/users/{username}/description", "otro")
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"hack\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("No podés editar el perfil de otro usuario."));

        verify(userProfiles).updateDescription(eq(EMAIL), eq("otro"), eq("hack"));
    }

    @Test
    void updateDescriptionRequiresAuthentication() throws Exception {
        mockMvc.perform(patch("/api/users/{username}/description", USERNAME)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"hola\"}"))
                .andExpect(status().isUnauthorized());

        verify(userProfiles, never()).updateDescription(any(), any(), any());
    }

    @Test
    void updateDescriptionValidatesLength() throws Exception {
        mockMvc.perform(patch("/api/users/{username}/description", USERNAME)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"" + "a".repeat(501) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "La descripción no puede superar los 500 caracteres."));

        verify(userProfiles, never()).updateDescription(any(), any(), any());
    }

    @Test
    void updateAvatarStoresTheImageForTheOwner() throws Exception {
        given(profileImages.updateAvatar(eq(EMAIL), eq(USERNAME), any()))
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION, AVATAR_URL));

        mockMvc.perform(putMultipart("/api/users/{username}/avatar", USERNAME)
                        .file(new MockMultipartFile("avatar", "foto.png", "image/png", PNG))
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").value(AVATAR_URL));
    }

    @Test
    void updateAvatarRejectsAnotherUserProfile() throws Exception {
        willThrow(new ProfileEditForbiddenException())
                .given(profileImages).updateAvatar(eq(EMAIL), eq("otro"), any());

        mockMvc.perform(putMultipart("/api/users/{username}/avatar", "otro")
                        .file(new MockMultipartFile("avatar", "foto.png", "image/png", PNG))
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error")
                        .value("No podés editar el perfil de otro usuario."));
    }

    @Test
    void updateAvatarRequiresAuthentication() throws Exception {
        mockMvc.perform(putMultipart("/api/users/{username}/avatar", USERNAME)
                        .file(new MockMultipartFile("avatar", "foto.png", "image/png", PNG))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(profileImages, never()).updateAvatar(any(), any(), any());
    }

    @Test
    void removeAvatarClearsTheImage() throws Exception {
        given(profileImages.removeAvatar(EMAIL, USERNAME))
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION, null));

        mockMvc.perform(delete("/api/users/{username}/avatar", USERNAME)
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.avatarUrl").doesNotExist());
    }

    @Test
    void removeAvatarRejectsAnotherUserProfile() throws Exception {
        willThrow(new ProfileEditForbiddenException())
                .given(profileImages).removeAvatar(EMAIL, "otro");

        mockMvc.perform(delete("/api/users/{username}/avatar", "otro")
                        .with(user(EMAIL))
                        .with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void removeAvatarRequiresAuthentication() throws Exception {
        mockMvc.perform(delete("/api/users/{username}/avatar", USERNAME).with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(profileImages, never()).removeAvatar(any(), any());
    }

    @Test
    void getAvatarIsPublicAndSendsTheImage() throws Exception {
        given(profileImages.readAvatar(USERNAME)).willReturn(ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff")
                .body(PNG));

        mockMvc.perform(get("/api/users/{username}/avatar", USERNAME))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(content().bytes(PNG));
    }

    @Test
    void getAvatarReturnsNotFoundWhenThereIsNoImage() throws Exception {
        given(profileImages.readAvatar(USERNAME))
                .willReturn(ResponseEntity.notFound().build());

        mockMvc.perform(get("/api/users/{username}/avatar", USERNAME))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsPublishedGamesForAUserPublicly() throws Exception {
        given(games.findByOwner_UsernameIgnoreCaseAndStatus(
                eq(USERNAME), eq(GameStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(List.of(game("game-1", "Un juego")),
                        PageRequest.of(0, 12), 1));

        mockMvc.perform(get("/api/users/{username}/games", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("game-1"))
                .andExpect(jsonPath("$.content[0].title").value("Un juego"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(12))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(games).findByOwner_UsernameIgnoreCaseAndStatus(eq(USERNAME),
                eq(GameStatus.PUBLISHED), eq(PageRequest.of(0, 12,
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    @Test
    void clampsUserGamePageAndSize() throws Exception {
        given(games.findByOwner_UsernameIgnoreCaseAndStatus(
                eq(USERNAME), eq(GameStatus.PUBLISHED), any()))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 24), 0));

        mockMvc.perform(get("/api/users/{username}/games", USERNAME)
                        .param("page", "-3")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(24));

        verify(games).findByOwner_UsernameIgnoreCaseAndStatus(eq(USERNAME),
                eq(GameStatus.PUBLISHED), eq(PageRequest.of(0, 24,
                        Sort.by(Sort.Direction.DESC, "createdAt"))));
    }

    private Game game(String id, String title) {
        User owner = new User(USERNAME, EMAIL, "{noop}encoded");
        Game game = new Game(id, owner, title, DESCRIPTION, "/cover.png",
                "index.html", GameStatus.PUBLISHED);
        ReflectionTestUtils.setField(game, "createdAt",
                java.time.Instant.parse("2026-10-16T12:00:00Z"));
        return game;
    }

    private static MockMultipartHttpServletRequestBuilder putMultipart(
            String urlTemplate,
            Object... uriVars) {

        return (MockMultipartHttpServletRequestBuilder) multipart(urlTemplate, uriVars)
                .with(request -> {
                    request.setMethod("PUT");
                    return request;
                });
    }
}
