package com.tip_video_ludoteca.comments;

public class CommentNotFoundException extends RuntimeException {

    public CommentNotFoundException() {
        super("El comentario no existe.");
    }
}