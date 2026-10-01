package com.tip_video_ludoteca.comments;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommentRequest(
        @NotBlank(message = "Escribí un comentario antes de publicarlo.")
        @Size(
            max = CommentService.MAX_CONTENT_LENGTH,
            message = "El comentario no puede superar los "
                    + CommentService.MAX_CONTENT_LENGTH + " caracteres.")
        String content,

        @Min(value = 1, message = "La valoración debe ser entre 1 y 5 estrellas.")
        @Max(value = 5, message = "La valoración debe ser entre 1 y 5 estrellas.")
        Integer rating) {
}