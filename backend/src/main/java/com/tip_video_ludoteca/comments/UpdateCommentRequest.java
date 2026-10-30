package com.tip_video_ludoteca.comments;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCommentRequest(
        @NotBlank(message = "Escribí un comentario antes de publicarlo.")
        @Size(
            max = CommentService.MAX_CONTENT_LENGTH,
            message = "El comentario no puede superar los "
                    + CommentService.MAX_CONTENT_LENGTH + " caracteres.")
        String content) {
}
