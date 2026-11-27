package com.tip_video_ludoteca.news;

public class NoticiaNotFoundException extends RuntimeException {

    public NoticiaNotFoundException() {
        super("La noticia no existe.");
    }

    public NoticiaNotFoundException(String message) {
        super(message);
    }
}
