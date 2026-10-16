package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.forums.ForumDiscussionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForumApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class ForumApiControllerTests {

    @MockitoBean
    private ForumDiscussionService discussions;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listRepliesIsPublic() throws Exception {
        given(discussions.listReplies(1L)).willReturn(List.of());

        mockMvc.perform(get("/api/forum/discussions/{discussionId}/replies", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void missingDiscussionReturnsNotFoundInsteadOfUnauthorized() throws Exception {
        willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND))
                .given(discussions).listReplies(1L);

        mockMvc.perform(get("/api/forum/discussions/{discussionId}/replies", 1))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingDiscussionsIsPublicAndSupportsPagination() throws Exception {
        given(discussions.listPublished(2, 5)).willReturn(
                new ForumDiscussionService.DiscussionPage(List.of(), 2, 5, 0, 0,
                        false, true));

        mockMvc.perform(get("/api/forum/discussions")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void creatingDiscussionRequiresAuthenticationAndCsrf() throws Exception {
        var form = multipart("/api/forum/discussions")
                .param("gameId", "game-1")
                .param("title", "Una discusión")
                .param("description", "Descripción breve")
                .param("tagIds", "1");

        mockMvc.perform(form.with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(multipart("/api/forum/discussions")
                        .param("gameId", "game-1")
                        .param("title", "Una discusión")
                        .param("description", "Descripción breve")
                        .param("tagIds", "1")
                        .with(user("author@ludarium.test")))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingDiscussionAllowsAnOptionalCoverForAuthenticatedUser() throws Exception {
        given(discussions.create("game-1", "Una discusión", "Descripción breve",
                null, List.of(1L), "author@ludarium.test"))
                .willReturn(new ForumDiscussionService.CreatedDiscussion(9L, "Una discusión"));

        mockMvc.perform(multipart("/api/forum/discussions")
                        .param("gameId", "game-1")
                        .param("title", "Una discusión")
                        .param("description", "Descripción breve")
                        .param("tagIds", "1")
                        .with(user("author@ludarium.test"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.title").value("Una discusión"));
    }

    @Test
    void replyingRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(post("/api/forum/discussions/1/replies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Respuesta\"}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/forum/discussions/1/replies")
                        .with(user("reader@ludarium.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Respuesta\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void ratingRequiresAuthenticationAndCsrf() throws Exception {
        mockMvc.perform(put("/api/forum/discussions/1/rating")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(put("/api/forum/discussions/1/rating")
                        .with(user("reader@ludarium.test"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4}"))
                .andExpect(status().isForbidden());
    }
}
