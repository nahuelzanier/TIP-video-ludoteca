package com.tip_video_ludoteca.games;

public class GameEditForbiddenException extends RuntimeException {

    public GameEditForbiddenException() {
        super("No podés publicar el juego de otro usuario.");
    }
}