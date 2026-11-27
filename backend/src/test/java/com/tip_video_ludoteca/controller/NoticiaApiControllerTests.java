package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.news.NoticiaEditForbiddenException;
import com.tip_video_ludoteca.news.NoticiaService;
import com.tip_video_ludoteca.news.NoticiaService.NewsImage;
import com.tip_video_ludoteca.news.NoticiaService.NewsPage;
import com.tip_video_ludoteca.news.NoticiaService.NewsResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.ServletWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NoticiaApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class NoticiaApiControllerTests {

    private static final String EMAIL = "author@test.com";
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @MockitoBean
    private NoticiaService noticiaService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publishesANewsWhenAuthenticated() throws Exception {
        given(noticiaService.create(eq("Titulo"), eq("Descripcion"), any(), eq(EMAIL)))
                .willReturn(response(1L, "Titulo"));

        mockMvc.perform(multipart("/api/news")
                        .file(new MockMultipartFile("image", "image.png", "image/png", PNG))
                        .param("title", "Titulo")
                        .param("description", "Descripcion")
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.authorUsername").value("author"));
    }

    @Test
    void rejectsPublishingWhenNotAuthenticated() throws Exception {
        mockMvc.perform(multipart("/api/news")
                        .param("title", "Titulo")
                        .param("description", "Descripcion")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void feedRequiresAuthentication() throws Exception {
        given(noticiaService.listFeed(EMAIL, PageRequest.of(0, 9)))
                .willReturn(new NewsPage(
                        List.of(response(1L, "Titulo")), 0, 9, 1, 1, false, false));

        mockMvc.perform(get("/api/news")
                        .with(user(EMAIL).password("secret").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Titulo"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/news"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void detailAndImageArePublic() throws Exception {
        given(noticiaService.getDetail(1L)).willReturn(response(1L, "Titulo"));
        given(noticiaService.getImage(1L)).willReturn(new NewsImage("image/png", PNG));

        mockMvc.perform(get("/api/news/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Titulo"));

        mockMvc.perform(get("/api/news/1/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    void deletingSomeonesElseNewsIsForbidden() throws Exception {
        doThrow(new NoticiaEditForbiddenException())
                .when(noticiaService).delete(eq(1L), eq(EMAIL));

        mockMvc.perform(delete("/api/news/1")
                        .with(user(EMAIL).password("secret").roles("USER"))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("No podés editar una noticia de otro usuario."));
    }

    private NewsResponse response(long id, String title) {
        return new NewsResponse(
                id,
                title,
                "Descripcion",
                Instant.parse("2026-01-01T10:00:00Z"),
                "author",
                null,
                null);
    }
}
