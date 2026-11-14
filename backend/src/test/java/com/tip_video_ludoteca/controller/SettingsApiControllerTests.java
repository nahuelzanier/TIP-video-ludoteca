package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.users.AccountSettingsService;
import com.tip_video_ludoteca.users.UserSettingsService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({UserSettingsController.class, AccountSettingsController.class})
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class SettingsApiControllerTests {

    @MockitoBean
    private UserSettingsService settings;

    @MockitoBean
    private AccountSettingsService accounts;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void readingSettingsRequiresAnAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/users/me/settings"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedUserCanReadAndUpdateOnlyTheirSettings() throws Exception {
        given(settings.get("player@example.test"))
                .willReturn(new UserSettingsService.SettingsResponse(70, "es", "dark"));
        given(settings.update("player@example.test",
                new UserSettingsService.UpdateSettingsRequest(70, "es", "dark")))
                .willReturn(new UserSettingsService.SettingsResponse(70, "es", "dark"));

        mockMvc.perform(get("/api/users/me/settings")
                        .with(user("player@example.test")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volume").value(70))
                .andExpect(jsonPath("$.language").value("es"))
                .andExpect(jsonPath("$.theme").value("dark"));

        mockMvc.perform(put("/api/users/me/settings")
                        .with(user("player@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volume\":70,\"language\":\"es\",\"theme\":\"dark\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.volume").value(70));
    }

    @Test
    void invalidSettingsAreRejectedByValidation() throws Exception {
        mockMvc.perform(put("/api/users/me/settings")
                        .with(user("player@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"volume\":140,\"language\":\"fr\",\"theme\":\"neon\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void usernameCanBeChangedThroughTheAuthenticatedAccountRoute() throws Exception {
        given(accounts.updateUsername("player@example.test", "new_name"))
                .willReturn(new AccountSettingsService.UsernameResponse("new_name"));

        mockMvc.perform(patch("/api/users/me/username")
                        .with(user("player@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new_name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("new_name"));
    }

    @Test
    void passwordCanBeChangedThroughTheAuthenticatedAccountRoute() throws Exception {
        mockMvc.perform(patch("/api/users/me/password")
                        .with(user("player@example.test"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"old-password\",\"newPassword\":\"new-password\"}"))
                .andExpect(status().isNoContent());
    }
}
