package com.tip_video_ludoteca.tags;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameTagServiceTests {

    private static final String GAME_ID = "game-1";
    private static final String EMAIL = "player@ludarium.test";

    @Mock
    private GameRepository games;

    @Mock
    private TagRepository tags;

    @Mock
    private GameTagAssignmentRepository assignments;

    @Mock
    private UserRepository users;

    @InjectMocks
    private GameTagService gameTags;

    @Test
    void assignTagPersistsTheUserAssignmentAndReturnsTheOrderedSummary() {
        Game game = game(GameStatus.PUBLISHED);
        User user = user();
        Tag tag = tag(5L, "Puzzle", "puzzle");
        List<GameTagSummary> summaries = List.of(summary(5L, "Puzzle", 1L));

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(tags.findById(5L)).thenReturn(Optional.of(tag));
        when(assignments.existsById_GameIdAndId_TagIdAndId_UserId(GAME_ID, 5L, 9L))
                .thenReturn(false);
        when(assignments.findTagSummariesByGameId(GAME_ID)).thenReturn(summaries);

        assertThat(gameTags.assignTag(GAME_ID, 5L, EMAIL)).isEqualTo(summaries);

        verify(assignments).saveAndFlush(any(GameTagAssignment.class));
        verify(assignments).findTagSummariesByGameId(GAME_ID);
    }

    @Test
    void assignTagRejectsARepeatedUserTagAssignment() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(GameStatus.PUBLISHED)));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(tags.findById(5L)).thenReturn(Optional.of(tag(5L, "Puzzle", "puzzle")));
        when(assignments.existsById_GameIdAndId_TagIdAndId_UserId(GAME_ID, 5L, 9L))
                .thenReturn(true);

        assertThatThrownBy(() -> gameTags.assignTag(GAME_ID, 5L, EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(assignments, never()).saveAndFlush(any(GameTagAssignment.class));
    }

    @Test
    void assignTagMapsConcurrentDuplicateInsertToConflict() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(GameStatus.PUBLISHED)));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(tags.findById(5L)).thenReturn(Optional.of(tag(5L, "Puzzle", "puzzle")));
        when(assignments.existsById_GameIdAndId_TagIdAndId_UserId(GAME_ID, 5L, 9L))
                .thenReturn(false);
        when(assignments.saveAndFlush(any(GameTagAssignment.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThatThrownBy(() -> gameTags.assignTag(GAME_ID, 5L, EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        verify(assignments, never()).findTagSummariesByGameId(GAME_ID);
    }

    @Test
    void assignTagDoesNotExposeDraftGames() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(GameStatus.DRAFT)));

        assertThatThrownBy(() -> gameTags.assignTag(GAME_ID, 5L, EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));

        verify(users, never()).findByEmailIgnoreCase(EMAIL);
        verify(assignments, never()).saveAndFlush(any(GameTagAssignment.class));
    }

    @Test
    void getMyAssignedTagIdsReturnsOnlyTheCurrentUsersAssignments() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(GameStatus.PUBLISHED)));
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(assignments.findAssignedTagIdsByGameIdAndUserId(GAME_ID, 9L))
                .thenReturn(List.of(2L, 5L));

        assertThat(gameTags.getMyAssignedTagIds(GAME_ID, EMAIL)).containsExactly(2L, 5L);
    }

    @Test
    void getAvailableTagsMapsTheCatalogInRepositoryOrder() {
        when(tags.findAllByOrderByNameAsc()).thenReturn(List.of(
                tag(2L, "Adventure", "adventure"),
                tag(5L, "Puzzle", "puzzle")));

        assertThat(gameTags.getAvailableTags())
                .extracting(GameTagService.TagOption::name)
                .containsExactly("Adventure", "Puzzle");
    }

    private Game game(GameStatus status) {
        return new Game(GAME_ID, user(), "Game", "", null, "index.html", status);
    }

    private User user() {
        User user = new User("player", EMAIL, "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", 9L);
        return user;
    }

    private Tag tag(Long id, String name, String slug) {
        Tag tag = new Tag();
        ReflectionTestUtils.setField(tag, "id", id);
        ReflectionTestUtils.setField(tag, "name", name);
        ReflectionTestUtils.setField(tag, "slug", slug);
        return tag;
    }

    private GameTagSummary summary(Long tagId, String name, Long count) {
        return new GameTagSummary(
                tagId,
                name,
                name.toLowerCase(),
                count,
                java.time.Instant.parse("2026-10-16T00:00:00Z"));
    }
}
