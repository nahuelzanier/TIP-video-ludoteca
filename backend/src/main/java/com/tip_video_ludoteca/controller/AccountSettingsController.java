package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.users.AccountSettingsService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
public class AccountSettingsController {

    private final AccountSettingsService accounts;

    public AccountSettingsController(AccountSettingsService accounts) {
        this.accounts = accounts;
    }

    @PatchMapping("/username")
    public AccountSettingsService.UsernameResponse updateUsername(
            @Valid @RequestBody UsernameRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return accounts.updateUsername(currentUser.getUsername(), request.username());
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @Valid @RequestBody PasswordRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        accounts.updatePassword(currentUser.getUsername(),
                request.currentPassword(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    public record UsernameRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_]{3,30}") String username) {
    }

    public record PasswordRequest(
            @NotBlank String currentPassword,
            @NotBlank @Size(min = 8, max = 72) String newPassword) {
    }
}
