package com.tip_video_ludoteca.games;

public class GameNotFoundException extends RuntimeException {

    public GameNotFoundException() {
        super("El juego no existe.");
    }
}