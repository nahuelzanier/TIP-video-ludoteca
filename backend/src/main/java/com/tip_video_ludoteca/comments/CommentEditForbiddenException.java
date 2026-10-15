package com.tip_video_ludoteca.comments;

public class CommentEditForbiddenException extends RuntimeException {

    public CommentEditForbiddenException() {
        super("No podés editar un comentario de otro usuario.");
    }
}
