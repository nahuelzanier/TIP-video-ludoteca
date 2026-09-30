package com.tip_video_ludoteca.users;

public class ProfileEditForbiddenException extends RuntimeException {

    public ProfileEditForbiddenException() {
        super("No podés editar el perfil de otro usuario.");
    }
}
