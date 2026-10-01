package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.comments.CommentResponse;
import com.tip_video_ludoteca.comments.CommentService;
import com.tip_video_ludoteca.comments.CreateCommentRequest;
import com.tip_video_ludoteca.comments.CreateReplyRequest;
import com.tip_video_ludoteca.comments.InvalidCommentException;
import com.tip_video_ludoteca.config.SecurityConfig;
import com.tip_video_ludoteca.games.GameNotFoundException;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CommentApiController.class)
@AutoConfigureMockMvc
@Import({SecurityConfig.class, SecurityAutoConfiguration.class,
        ServletWebSecurityAutoConfiguration.class})
@TestPropertySource(properties = "app.frontend-origin=http://localhost:5173")
class CommentApiControllerTests {

    private static final String EMAIL = "lucas2@gmail.com";
    private static final String GAME_ID = "game-1";

    @MockitoBean
    private CommentService comments;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void listCommentsIsPublic() throws Exception {
        given(comments.listByGame(GAME_ID)).willReturn(List.of(comment(1L, 5, true, List.of())));

        mockMvc.perform(get("/api/games/{gameId}/comments", GAME_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].content").value("Gran juego."))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[0].username").value("lucas2"))
                .andExpect(jsonPath("$[0].isGameAuthor").value(true))
                .andExpect(jsonPath("$[0].replies").isArray())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    void listCommentsReturnsNotFoundForUnknownGame() throws Exception {
        given(comments.listByGame("noexiste")).willThrow(new GameNotFoundException());

        mockMvc.perform(get("/api/games/{gameId}/comments", "noexiste"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("El juego no existe."));
    }

    @Test
    void createCommentUsesTheAuthenticatedUser() throws Exception {
        given(comments.create(eq(GAME_ID), any(CreateCommentRequest.class), eq(EMAIL)))
                .willReturn(comment(2L, null, false, List.of()));

        mockMvc.perform(post("/api/games/{gameId}/comments", GAME_ID)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Gran juego.\",\"rating\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.isGameAuthor").value(false));

        verify(comments).create(
                eq(GAME_ID),
                eq(new CreateCommentRequest("Gran juego.", 5)),
                eq(EMAIL));
    }

    @Test
    void createCommentRejectsRatingOutOfRange() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/comments", GAME_ID)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Gran juego.\",\"rating\":6}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("La valoración debe ser entre 1 y 5 estrellas."));

        verify(comments, never()).create(any(), any(), any());
    }

    @Test
    void createCommentRejectsBlankContent() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/comments", GAME_ID)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Escribí un comentario antes de publicarlo."));

        verify(comments, never()).create(any(), any(), any());
    }

    @Test
    void createCommentRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/games/{gameId}/comments", GAME_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Gran juego.\"}"))
                .andExpect(status().isUnauthorized());

        verify(comments, never()).create(any(), any(), any());
    }

    @Test
    void createReplyReturnsTheNestedComment() throws Exception {
        given(comments.createReply(eq(1L), any(CreateReplyRequest.class), eq(EMAIL)))
                .willReturn(comment(3L, null, true, List.of()));

        mockMvc.perform(post("/api/comments/{commentId}/replies", 1L)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Totalmente de acuerdo.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.username").value("lucas2"));
    }

    @Test
    void createReplyRejectsRating() throws Exception {
        willThrow(new InvalidCommentException(
                "Las respuestas no pueden tener valoración. Valorá el comentario original."))
                .given(comments).createReply(any(), any(), any());

        mockMvc.perform(post("/api/comments/{commentId}/replies", 1L)
                        .with(user(EMAIL))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Con estrella\",\"rating\":4}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Las respuestas no pueden tener valoración. Valorá el comentario original."));
    }

    @Test
    void createReplyRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/comments/{commentId}/replies", 1L)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hola\"}"))
                .andExpect(status().isUnauthorized());

        verify(comments, never()).createReply(any(), any(), any());
    }

    private CommentResponse comment(
            Long id,
            Integer rating,
            boolean gameAuthor,
            List<CommentResponse> replies) {

        return new CommentResponse(
                id,
                "Gran juego.",
                rating,
                null,
                "lucas2",
                null,
                gameAuthor,
                replies);
    }
}