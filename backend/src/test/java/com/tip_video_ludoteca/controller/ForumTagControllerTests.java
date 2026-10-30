package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.forums.ForumTagService;
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

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForumTagController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class ForumTagControllerTests {

    @MockitoBean
    private ForumTagService tags;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void forumTagCatalogIsPublicAndReturnsItsOwnTags() throws Exception {
        given(tags.listTags()).willReturn(List.of(
                new ForumTagService.ForumTagOption(1L, "Discussion", "discussion"),
                new ForumTagService.ForumTagOption(2L, "Bug", "bug")
        ));

        mockMvc.perform(get("/api/forum/tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Discussion"))
                .andExpect(jsonPath("$[0].slug").value("discussion"))
                .andExpect(jsonPath("$[1].name").value("Bug"));
    }
}