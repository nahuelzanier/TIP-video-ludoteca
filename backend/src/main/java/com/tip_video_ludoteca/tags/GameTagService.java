package com.tip_video_ludoteca.tags;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GameTagService {

    private final GameRepository games;
    private final TagRepository tags;
    private final GameTagAssignmentRepository assignments;
    private final UserRepository users;

    public GameTagService(
            GameRepository games,
            TagRepository tags,
            GameTagAssignmentRepository assignments,
            UserRepository users) {
        this.games = games;
        this.tags = tags;
        this.assignments = assignments;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<TagOption> getAvailableTags() {
        return tags.findAllByOrderByNameAsc()
                .stream()
                .map(tag -> new TagOption(tag.getId(), tag.getName(), tag.getSlug()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GameTagSummary> getGameTags(String gameId) {
        requirePublishedGame(gameId);
        return assignments.findTagSummariesByGameId(gameId);
    }

    @Transactional
    public List<GameTagSummary> assignTag(
            String gameId,
            Long tagId,
            String userEmail) {

        if (tagId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A tag ID is required."
            );
        }

        Game game = requirePublishedGame(gameId);

        User user = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));

        Tag tag = tags.findById(tagId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tag not found."
                ));

        if (assignments.existsById_GameIdAndId_TagIdAndId_UserId(
                gameId, tagId, user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already assigned this tag to this game."
            );
        }

        try {
            assignments.saveAndFlush(new GameTagAssignment(game, tag, user));
        } catch (DataIntegrityViolationException exception) {
            // También cubre dos solicitudes simultáneas del mismo usuario.
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already assigned this tag to this game.",
                    exception
            );
        }

        return assignments.findTagSummariesByGameId(gameId);
    }

    @Transactional(readOnly = true)
    public List<Long> getMyAssignedTagIds(String gameId, String userEmail) {
        requirePublishedGame(gameId);

        User user = users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Authenticated user not found."
                ));

        return assignments.findAssignedTagIdsByGameIdAndUserId(
                gameId,
                user.getId()
        );
    }

    private Game requirePublishedGame(String gameId) {
        Game game = games.findById(gameId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Game not found."
                ));

        if (game.getStatus() != GameStatus.PUBLISHED) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Game not found."
            );
        }

        return game;
    }

    public record TagOption(Long id, String name, String slug) {
    }
}