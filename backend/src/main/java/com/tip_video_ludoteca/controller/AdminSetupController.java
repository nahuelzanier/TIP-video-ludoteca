package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/admin/setup")
public class AdminSetupController {

    private final UserRepository users;
    private final String setupToken;
    private final SecurityContextRepository securityContextRepository;

    public AdminSetupController(
            UserRepository users,
            @Value("${app.admin.setup-token:}") String setupToken,
            SecurityContextRepository securityContextRepository) {
        this.users = users;
        this.setupToken = setupToken;
        this.securityContextRepository = securityContextRepository;
    }

    @GetMapping("/status")
    public SetupStatus status() {
        return new SetupStatus(!setupToken.isBlank() && users.countByRole("ADMIN") == 0);
    }

    @PostMapping
    @Transactional
    public SetupResult setup(
            @Valid @RequestBody SetupRequest request,
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        if (setupToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "La configuración inicial de admin no está habilitada.");
        }
        if (users.countByRole("ADMIN") > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya existe un administrador. Pídele que te habilite desde la bandeja admin.");
        }
        boolean validToken = MessageDigest.isEqual(
                setupToken.getBytes(StandardCharsets.UTF_8),
                request.token().getBytes(StandardCharsets.UTF_8));
        if (!validToken) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La clave de configuración no es válida.");
        }

        User user = users.findByEmailIgnoreCase(principal.getUsername())
                .orElseThrow(UserNotFoundException::new);
        user.promoteToAdmin();

        UserDetails adminPrincipal = org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                adminPrincipal, null, adminPrincipal.getAuthorities()));
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        return new SetupResult(user.getUsername(), user.getEmail(), user.getRole());
    }

    public record SetupRequest(@NotBlank String token) { }

    public record SetupResult(String username, String email, String role) { }

    public record SetupStatus(boolean available) { }
}
