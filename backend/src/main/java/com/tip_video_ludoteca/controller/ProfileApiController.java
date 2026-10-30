package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.users.UpdateDescriptionRequest;
import com.tip_video_ludoteca.users.UserProfileImageService;
import com.tip_video_ludoteca.users.UserProfileResponse;
import com.tip_video_ludoteca.users.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class ProfileApiController {

    private final UserProfileService userProfiles;
    private final UserProfileImageService profileImages;
    private final GameRepository games;

    public ProfileApiController(
            UserProfileService userProfiles,
            UserProfileImageService profileImages,
            GameRepository games) {
        this.userProfiles = userProfiles;
        this.profileImages = profileImages;
        this.games = games;
    }

    @GetMapping("/{username}/profile")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userProfiles.getProfile(username);
    }

    @GetMapping("/{username}/games")
        public GamePage getUserGames(
                @PathVariable String username,
                @RequestParam(defaultValue = "0") int page,
                @RequestParam(defaultValue = "12") int size) {

            int safePage = Math.max(page, 0);
            int safeSize = Math.min(Math.max(size, 1), 24);

            Page<Game> result = games.findByOwner_UsernameIgnoreCaseAndStatus(
                    username,
                    GameStatus.PUBLISHED,
                    PageRequest.of(
                            safePage,
                            safeSize,
                            Sort.by(Sort.Direction.DESC, "createdAt")
                    )
            );

            List<GameSummary> content = result.getContent().stream()
                    .map(game -> new GameSummary(
                            game.getId(),
                            game.getTitle(),
                            game.getCoverImageUrl(),
                            game.getDescription()
                    ))
                    .toList();

            return new GamePage(
                    content,
                    result.getNumber(),
                    result.getSize(),
                    result.getTotalElements(),
                    result.getTotalPages(),
                    result.hasNext(),
                    result.hasPrevious()
            );
        }

        public record GameSummary(
                String id,
                String title,
                String image,
                String description
        ) {}

        public record GamePage(
                List<GameSummary> content,
                int page,
                int size,
                long totalElements,
                int totalPages,
                boolean hasNext,
                boolean hasPrevious
        ) {}

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

    @PutMapping(value = "/{username}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse updateAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String username,
            @RequestParam("avatar") MultipartFile avatar) {

        return profileImages.updateAvatar(principal.getUsername(), username, avatar);
    }

    @DeleteMapping("/{username}/avatar")
    public UserProfileResponse removeAvatar(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable String username) {

        return profileImages.removeAvatar(principal.getUsername(), username);
    }

    @GetMapping("/{username}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable String username) {
        return profileImages.readAvatar(username);
    }
}
