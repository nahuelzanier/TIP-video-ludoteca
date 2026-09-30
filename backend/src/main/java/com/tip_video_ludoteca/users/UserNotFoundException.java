package com.tip_video_ludoteca.users;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException() {
        super("Usuario no encontrado.");
    }
}
