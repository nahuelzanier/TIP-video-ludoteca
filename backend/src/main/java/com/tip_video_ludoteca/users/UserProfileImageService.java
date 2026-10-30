package com.tip_video_ludoteca.users;

import com.tip_video_ludoteca.media.ImageValidation;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@Service
public class UserProfileImageService {

    public static final long MAX_AVATAR_BYTES = 2L * 1024 * 1024;

    private final UserRepository users;

    public UserProfileImageService(UserRepository users) {
        this.users = users;
    }

    @Transactional
    public UserProfileResponse updateAvatar(
            String authenticatedEmail,
            String username,
            MultipartFile file) {

        User user = ownedUser(authenticatedEmail, username);
        byte[] content = readImage(file);

        String contentType;
        try {
            contentType = ImageValidation.detectContentType(content);
        } catch (ImageValidation.RejectedImageException exception) {
            throw badRequest("La foto de perfil debe ser un archivo PNG o JPEG.");
        }

        user.updateProfileImage(content, contentType);

        return UserProfileResponse.from(users.save(user));
    }

    @Transactional
    public UserProfileResponse removeAvatar(String authenticatedEmail, String username) {
        User user = ownedUser(authenticatedEmail, username);

        user.clearProfileImage();

        return UserProfileResponse.from(users.save(user));
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> readAvatar(String username) {
        User user = users.findByUsernameIgnoreCase(username)
                .orElseThrow(UserNotFoundException::new);

        if (!user.hasProfileImage()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(user.getProfileImageContentType()))
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noCache())
                .body(user.getProfileImage());
    }

    private User ownedUser(String authenticatedEmail, String username) {
        User user = users.findByEmailIgnoreCase(authenticatedEmail)
                .orElseThrow(UserNotFoundException::new);

        if (!user.getUsername().equalsIgnoreCase(username)) {
            throw new ProfileEditForbiddenException();
        }

        return user;
    }

    private byte[] readImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw badRequest("Elegí una foto de perfil.");
        }

        if (file.getSize() > MAX_AVATAR_BYTES) {
            throw badRequest("La foto de perfil no puede superar los 2 MB.");
        }

        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "No se pudo leer la foto de perfil.",
                    exception
            );
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}