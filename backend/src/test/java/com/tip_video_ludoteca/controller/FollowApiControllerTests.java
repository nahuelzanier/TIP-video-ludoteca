package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.followers.FollowService;
import com.tip_video_ludoteca.followers.InvalidFollowException;
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
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FollowApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class FollowApiControllerTests {

    private static final String EMAIL = "user@test.com";
    private static final String USERNAME = "creator";

    @MockitoBean
    private FollowService followService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void followsAUserWhenAuthenticated() throws Exception {
        mockMvc.perform(put("/api/follows/{username}", USERNAME)
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(USERNAME))
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void rejectsFollowWhenNotAuthenticated() throws Exception {
        mockMvc.perform(put("/api/follows/{username}", USERNAME).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsBadRequestWhenFollowingYourself() throws Exception {
        doThrow(new InvalidFollowException("No podés seguirte a vos mismo."))
                .when(followService).follow(USERNAME, EMAIL);

        mockMvc.perform(put("/api/follows/{username}", USERNAME)
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No podés seguirte a vos mismo."));
    }

    @Test
    void unfollowsAUserWhenAuthenticated() throws Exception {
        mockMvc.perform(delete("/api/follows/{username}", USERNAME)
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void reportsTheFollowStatus() throws Exception {
        given(followService.isFollowing(USERNAME, EMAIL)).willReturn(true);

        mockMvc.perform(get("/api/follows/{username}/status", USERNAME)
                        .with(user(EMAIL).password("secret").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    void countsArePublic() throws Exception {
        given(followService.counts(USERNAME))
                .willReturn(new FollowService.FollowCounts(USERNAME, 12L, 4L));

        mockMvc.perform(get("/api/follows/{username}/counts", USERNAME))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followersCount").value(12))
                .andExpect(jsonPath("$.followingCount").value(4));
    }
}
