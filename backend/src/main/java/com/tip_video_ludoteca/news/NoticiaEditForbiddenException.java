package com.tip_video_ludoteca.news;

public class NoticiaEditForbiddenException extends RuntimeException {

    public NoticiaEditForbiddenException() {
        super("No podés editar una noticia de otro usuario.");
    }
}
