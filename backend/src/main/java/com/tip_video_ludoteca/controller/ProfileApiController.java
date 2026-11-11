package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.favorites.FavoriteService;
import com.tip_video_ludoteca.forums.ForumDiscussionRepository;
import com.tip_video_ludoteca.forums.ForumDiscussion;
import com.tip_video_ludoteca.forums.ForumDiscussionService;
import com.tip_video_ludoteca.forums.ForumRatingAggregate;
import com.tip_video_ludoteca.forums.ForumRatingRepository;
import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class ProfileApiController {

    private final UserProfileService userProfiles;
    private final UserProfileImageService profileImages;
    private final GameRepository games;
    private final FavoriteService favorites;
    private final ForumDiscussionRepository discussions;
    private final ForumRatingRepository forumRatings;

    public ProfileApiController(
            UserProfileService userProfiles,
            UserProfileImageService profileImages,
            GameRepository games,
            FavoriteService favorites,
            ForumDiscussionRepository discussions,
            ForumRatingRepository forumRatings) {
        this.userProfiles = userProfiles;
        this.profileImages = profileImages;
        this.games = games;
        this.favorites = favorites;
        this.discussions = discussions;
        this.forumRatings = forumRatings;
    }

    @GetMapping("/{username}/profile")
    public UserProfileResponse getProfile(@PathVariable String username) {
        return userProfiles.getProfile(username);
    }

    @GetMapping("/{username}/forums")
    @Transactional(readOnly = true)
    public List<ForumDiscussionService.DiscussionSummary> getUserForums(
            @PathVariable String username) {
        List<ForumDiscussion> authoredDiscussions = discussions
                .findByAuthorUsernameAndGameStatus(username, GameStatus.PUBLISHED);
        if (authoredDiscussions.isEmpty()) return List.of();

        List<Long> discussionIds = authoredDiscussions.stream()
                .map(ForumDiscussion::getId)
                .toList();
        Map<Long, ForumRatingAggregate> ratingByDiscussion = new HashMap<>();
        forumRatings.aggregateByDiscussionIds(discussionIds).forEach(rating ->
                ratingByDiscussion.put(rating.getDiscussionId(), rating));

        return authoredDiscussions.stream()
                .map(discussion -> {
                    ForumRatingAggregate rating = ratingByDiscussion.get(discussion.getId());
                    return new ForumDiscussionService.DiscussionSummary(
                            discussion.getId(),
                            discussion.getTitle(),
                            discussion.getDescription(),
                            discussion.getCreatedAt(),
                            discussion.getGame().getId(),
                            discussion.getGame().getTitle(),
                            discussion.getAuthor().getUsername(),
                            discussion.getAuthor().getProfileImageUrl(),
                            discussion.getCoverImage() == null
                                    ? null
                                    : "/api/forum/discussions/" + discussion.getId() + "/cover",
                            discussion.getTags().stream()
                                    .map(tag -> new ForumDiscussionService.DiscussionTag(
                                            tag.getId(), tag.getName(), tag.getSlug()))
                                    .toList(),
                            rating == null ? null : rating.getAverageRating(),
                            rating == null ? 0 : rating.getRatingCount());
                })
                .toList();
    }

    @GetMapping("/{username}/games")
    public GamePage getUserGames(
            @PathVariable String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserDetails principal) {

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

        Set<String> favoriteIds = principal == null
                ? Set.of()
                : favorites.findFavoriteGameIdsForUser(principal.getUsername());

        List<GameSummary> content = result.getContent().stream()
                .map(game -> new GameSummary(
                        game.getId(),
                        game.getTitle(),
                        game.getCoverImageUrl(),
                        game.getDescription(),
                        favoriteIds.contains(game.getId())
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
            String description,
            boolean favorito
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
