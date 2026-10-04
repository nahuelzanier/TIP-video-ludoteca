package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.users.ProfileEditForbiddenException;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserProfileImageService;
import com.tip_video_ludoteca.users.UserProfileResponse;
import com.tip_video_ludoteca.users.UserProfileService;
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
    private com.tip_video_ludoteca.games.GameRepository games;

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
