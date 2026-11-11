package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserRepository users;

    public AdminUserController(UserRepository users) {
        this.users = users;
    }

    @PostMapping("/promote")
    @Transactional
    public AdminUser promote(@Valid @RequestBody PromoteAdminRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(UserNotFoundException::new);
        user.promoteToAdmin();
        return new AdminUser(user.getUsername(), user.getEmail(), user.getRole());
    }

    public record PromoteAdminRequest(
            @NotBlank @Email @Size(max = 254) String email) { }

    public record AdminUser(String username, String email, String role) { }
}
