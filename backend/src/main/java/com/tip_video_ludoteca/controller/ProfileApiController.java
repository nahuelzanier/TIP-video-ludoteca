package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.users.UpdateDescriptionRequest;
import com.tip_video_ludoteca.users.UserProfileResponse;
import com.tip_video_ludoteca.users.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class ProfileApiController {

    private final UserProfileService userProfiles;

    public ProfileApiController(UserProfileService userProfiles) {
        this.userProfiles = userProfiles;
    }

    @GetMapping("/{username}/profile")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userProfiles.getProfile(username);
    }

    @PatchMapping("/{username}/description")
    public UserProfileResponse updateDescription(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String username,
            @Valid @RequestBody UpdateDescriptionRequest request) {

        return userProfiles.updateDescription(
                principal.getUsername(),
                username,
                request.description());
    }
}
