package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.users.UserSettingsService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me/settings")
public class UserSettingsController {

    private final UserSettingsService settings;

    public UserSettingsController(UserSettingsService settings) {
        this.settings = settings;
    }

    @GetMapping
    public UserSettingsService.SettingsResponse get(
            @AuthenticationPrincipal UserDetails currentUser) {
        return settings.get(currentUser.getUsername());
    }

    @PutMapping
    public UserSettingsService.SettingsResponse update(
            @Valid @RequestBody UserSettingsService.UpdateSettingsRequest request,
            @AuthenticationPrincipal UserDetails currentUser) {
        return settings.update(currentUser.getUsername(), request);
    }
}
