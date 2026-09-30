package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.users.ProfileEditForbiddenException;
import com.tip_video_ludoteca.users.UserNotFoundException;
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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @MockitoBean
    private UserProfileService userProfiles;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProfileIsPublic() throws Exception {
        given(userProfiles.getProfile(USERNAME))
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION));

        mockMvc.perform(get("/api/users/{username}/profile", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.description").value(DESCRIPTION))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
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
                .willReturn(new UserProfileResponse(1L, USERNAME, DESCRIPTION));

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
}
