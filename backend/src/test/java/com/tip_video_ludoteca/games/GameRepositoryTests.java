package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.comments.Comment;
import com.tip_video_ludoteca.comments.CommentRepository;
import com.tip_video_ludoteca.tags.GameTagAssignment;
import com.tip_video_ludoteca.tags.GameTagAssignmentRepository;
import com.tip_video_ludoteca.tags.Tag;
import com.tip_video_ludoteca.tags.TagRepository;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameRepositoryTests {

    @Autowired
    private GameRepository games;

    @Autowired
    private UserRepository users;

    @Autowired
    private TagRepository tags;

    @Autowired
    private GameTagAssignmentRepository assignments;

    @Autowired
    private CommentRepository comments;

    @BeforeEach
    void clearGames() {
        games.deleteAll();
    }

    @Test
    void findRandomByStatusReturnsAPublishedGame() {
        User owner = users.save(
                new User("random-owner", "random-owner@test.com", "{bcrypt}hash"));
        games.save(game("repo-test-draft", owner, GameStatus.DRAFT));
        games.save(game("repo-test-published", owner, GameStatus.PUBLISHED));

        var result = games.findRandomByStatus("PUBLISHED");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo("repo-test-published");
        assertThat(result.get().getStatus()).isEqualTo(GameStatus.PUBLISHED);
    }

    @Test
    void findRandomByStatusIgnoresDraftGames() {
        User owner = users.save(
                new User("random-draft-owner", "random-draft-owner@test.com", "{bcrypt}hash"));
        games.save(game("repo-test-only-draft", owner, GameStatus.DRAFT));

        assertThat(games.findRandomByStatus("PUBLISHED")).isEmpty();
    }

    @Test
    void findRandomByStatusReturnsEmptyWhenThereAreNoGames() {
        games.deleteAll();

        assertThat(games.findRandomByStatus("PUBLISHED")).isEmpty();
    }

    @Test
    void combinesTitleAllSelectedTagsAndMinimumAverageRatingForPublishedGames() {
        User owner = users.save(new User("filter-owner", "filter-owner@test.com", "{noop}hash"));
        User player = users.save(new User("filter-player", "filter-player@test.com", "{noop}hash"));

        Tag puzzle = tags.save(tag("Puzzle", "puzzle"));
        Tag action = tags.save(tag("Action", "action"));

        Game matching = games.saveAndFlush(game(
                "filter-matching", owner, "Puzzle Adventure", GameStatus.PUBLISHED));
        Game lowRated = games.saveAndFlush(game(
                "filter-low-rated", owner, "Puzzle Battle", GameStatus.PUBLISHED));
        Game singleTag = games.saveAndFlush(game(
                "filter-single-tag", owner, "Puzzle Solo", GameStatus.PUBLISHED));
        Game draft = games.saveAndFlush(game(
                "filter-draft", owner, "Puzzle Draft", GameStatus.DRAFT));

        assignments.saveAll(List.of(
                new GameTagAssignment(matching, action, player),
                new GameTagAssignment(matching, puzzle, owner),
                new GameTagAssignment(lowRated, puzzle, player),
                new GameTagAssignment(singleTag, puzzle, player),
                new GameTagAssignment(draft, action, player)
        ));

        comments.save(new Comment(matching, player, null, "Excellent", 5));
        comments.save(new Comment(matching, player, null, "Very good", 4));
        comments.save(new Comment(singleTag, player, null, "Great but missing one tag", 5));
        Comment lowRating = comments.saveAndFlush(
                new Comment(lowRated, player, null, "Average", 3));
        comments.save(new Comment(lowRated, player, lowRating, "A reply rating is ignored", 5));
        comments.save(new Comment(draft, player, null, "Excellent", 5));

        Page<Game> result = games.findAll(
                GameSpecifications.withFilters("puzzle", List.of(puzzle.getId(), action.getId()), 4.5),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Game::getId)
                .containsExactly("filter-matching");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void withoutTagOrRatingFiltersReturnsAllPublishedGamesMatchingTitle() {
        User owner = users.save(new User("plain-owner", "plain-owner@test.com", "{noop}hash"));
        games.save(game("plain-published", owner, "Puzzle One", GameStatus.PUBLISHED));
        games.save(game("plain-draft", owner, "Puzzle Two", GameStatus.DRAFT));
        games.save(game("plain-other", owner, "Other Game", GameStatus.PUBLISHED));

        Page<Game> result = games.findAll(
                GameSpecifications.withFilters("puzzle", List.of(), null),
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent())
                .extracting(Game::getId)
                .containsExactly("plain-published");
    }

    private Game game(String id, User owner, GameStatus status) {
        return game(id, owner, "Juego " + id, status);
    }

    private Game game(String id, User owner, String title, GameStatus status) {
        return new Game(id, owner, title, "", null, "index.html", status);
    }

    private Tag tag(String name, String slug) {
        try {
            Constructor<Tag> constructor = Tag.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            Tag tag = constructor.newInstance();
            ReflectionTestUtils.setField(tag, "name", name);
            ReflectionTestUtils.setField(tag, "slug", slug);
            return tag;
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Could not create test tag", exception);
        }
    }
}
