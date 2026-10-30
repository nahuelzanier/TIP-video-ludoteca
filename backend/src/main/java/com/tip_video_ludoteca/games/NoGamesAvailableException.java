package com.tip_video_ludoteca.games;

public class NoGamesAvailableException extends RuntimeException {

    public NoGamesAvailableException() {
        super("There are no games available yet.");
    }
}
