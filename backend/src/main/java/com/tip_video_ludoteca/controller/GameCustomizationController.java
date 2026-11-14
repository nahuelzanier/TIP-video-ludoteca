package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameCustomizationService;
import com.tip_video_ludoteca.games.UpdateGameCustomizationRequest;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/games/{gameId}/customization")
public class GameCustomizationController {

    private final GameCustomizationService customizations;

    public GameCustomizationController(GameCustomizationService customizations) {
        this.customizations = customizations;
    }

    @GetMapping
    public GameCustomizationService.Customization get(@PathVariable String gameId) {
        return customizations.get(gameId);
    }

    @PutMapping
    public GameCustomizationService.Customization update(
            @PathVariable String gameId,
            @Valid @RequestBody UpdateGameCustomizationRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return customizations.update(gameId, principal.getUsername(), request);
    }
}
