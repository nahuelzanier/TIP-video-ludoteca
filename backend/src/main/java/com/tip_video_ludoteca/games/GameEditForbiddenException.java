package com.tip_video_ludoteca.games;

public class GameEditForbiddenException extends RuntimeException {

    public GameEditForbiddenException() {
        super("No podés editar la página de otro usuario.");
    }
}
