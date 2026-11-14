package com.tip_video_ludoteca.users;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AccountSettingsService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AccountSettingsService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsernameResponse updateUsername(String email, String requestedUsername) {
        User user = findUser(email);
        String username = requestedUsername.trim().toLowerCase(Locale.ROOT);

        if (!user.getUsername().equalsIgnoreCase(username)
                && users.existsByUsernameIgnoreCase(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ese nombre de usuario ya existe.");
        }

        user.updateUsername(username);
        return new UsernameResponse(username);
    }

    @Transactional
    public void updatePassword(String email, String currentPassword, String newPassword) {
        User user = findUser(email);
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La contraseña actual no es correcta.");
        }
        user.updatePasswordHash(passwordEncoder.encode(newPassword));
    }

    private User findUser(String email) {
        return users.findByEmailIgnoreCase(email)
                .orElseThrow(UserNotFoundException::new);
    }

    public record UsernameResponse(String username) {
    }
}
