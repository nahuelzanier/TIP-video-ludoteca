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
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
}
