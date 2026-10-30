package com.tip_video_ludoteca.tags;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRecommendationSummary;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameTagAssignmentRepositoryTests {

    @Autowired
    private GameTagAssignmentRepository assignments;

    @Autowired
    private GameRepository games;

    @Autowired
    private UserRepository users;

    @Autowired
    private TagRepository tags;

    private User owner;

    @BeforeEach
    void cleanSlate() {
        assignments.deleteAll();
        games.deleteAll();
        owner = users.save(
                new User("rec-owner", "rec-owner@test.com", "{bcrypt}hash"));
    }

    @Test
    void returnsEmptyWhenNoTagsAreShared() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game other = game("rec-test-other", GameStatus.PUBLISHED);
        assign(other, tag("racing"), owner);
        assign(other, tag("horror"), owner);

        assertThat(recommendationsFor(current)).isEmpty();
    }

    @Test
    void returnsEmptyWhenOnlyOneTagIsShared() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game other = game("rec-test-other", GameStatus.PUBLISHED);
        assign(other, tag("action"), owner);
        assign(other, tag("racing"), owner);

        assertThat(recommendationsFor(current)).isEmpty();
    }

    @Test
    void returnsGamesSharingAtLeastTwoTags() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game close = game("rec-test-close", GameStatus.PUBLISHED);
        assign(close, tag("action"), owner);
        assign(close, tag("adventure"), owner);

        List<GameRecommendationSummary> result = recommendationsFor(current);

        assertThat(result).extracting(GameRecommendationSummary::id)
                .containsExactly("rec-test-close");
        assertThat(result.get(0).title()).isEqualTo("Juego rec-test-close");
        assertThat(result.get(0).image()).isNull();
    }

    @Test
    void ignoresGamesWithOnlyOneSharedTagAmongOtherMatches() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game match = game("rec-test-match", GameStatus.PUBLISHED);
        assign(match, tag("action"), owner);
        assign(match, tag("adventure"), owner);

        Game partial = game("rec-test-partial", GameStatus.PUBLISHED);
        assign(partial, tag("action"), owner);
        assign(partial, tag("racing"), owner);

        assertThat(recommendationsFor(current))
                .extracting(GameRecommendationSummary::id)
                .containsExactly("rec-test-match");
    }

    @Test
    void returnsAtMostThreeCandidatesOrderedBySharedTagCount() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);
        assign(current, tag("puzzle"), owner);
        assign(current, tag("racing"), owner);

        // Comparte los 4 tags → primer puesto.
        Game best = game("rec-test-a", GameStatus.PUBLISHED);
        assign(best, tag("action"), owner);
        assign(best, tag("adventure"), owner);
        assign(best, tag("puzzle"), owner);
        assign(best, tag("racing"), owner);

        // Comparte solo 2 → no llega al podio.
        Game worst = game("rec-test-b", GameStatus.PUBLISHED);
        assign(worst, tag("action"), owner);
        assign(worst, tag("adventure"), owner);

        // Empatan en 3 (pierden el desempate por título: c < d < e).
        Game tiedC = game("rec-test-c", GameStatus.PUBLISHED);
        assign(tiedC, tag("action"), owner);
        assign(tiedC, tag("adventure"), owner);
        assign(tiedC, tag("puzzle"), owner);

        Game tiedD = game("rec-test-d", GameStatus.PUBLISHED);
        assign(tiedD, tag("action"), owner);
        assign(tiedD, tag("adventure"), owner);
        assign(tiedD, tag("puzzle"), owner);

        Game tiedE = game("rec-test-e", GameStatus.PUBLISHED);
        assign(tiedE, tag("action"), owner);
        assign(tiedE, tag("adventure"), owner);
        assign(tiedE, tag("puzzle"), owner);

        List<GameRecommendationSummary> result = recommendationsFor(current);

        assertThat(result).hasSize(3);
        assertThat(result).extracting(GameRecommendationSummary::id)
                .containsExactly("rec-test-a", "rec-test-c", "rec-test-d");
    }

    @Test
    void excludesTheCurrentGameEvenWhenItSharesManyTags() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);
        assign(current, tag("puzzle"), owner);
        assign(current, tag("racing"), owner);

        assertThat(recommendationsFor(current)).isEmpty();
    }

    @Test
    void breaksTiesDeterministicallyByTitleThenId() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game second = game("rec-test-b", GameStatus.PUBLISHED);
        assign(second, tag("action"), owner);
        assign(second, tag("adventure"), owner);

        Game first = game("rec-test-a", GameStatus.PUBLISHED);
        assign(first, tag("action"), owner);
        assign(first, tag("adventure"), owner);

        List<GameRecommendationSummary> result = recommendationsFor(current);

        // Empate en cantidad de tags → ordena por título ascendente.
        assertThat(result).extracting(GameRecommendationSummary::id)
                .containsExactly("rec-test-a", "rec-test-b");
    }

    @Test
    void breaksTitleTiesByGameId() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game zeta = games.save(new Game(
                "rec-test-z", owner, "Mismo título", "", null,
                "index.html", GameStatus.PUBLISHED));
        assign(zeta, tag("action"), owner);
        assign(zeta, tag("adventure"), owner);

        Game why = games.save(new Game(
                "rec-test-y", owner, "Mismo título", "", null,
                "index.html", GameStatus.PUBLISHED));
        assign(why, tag("action"), owner);
        assign(why, tag("adventure"), owner);

        List<GameRecommendationSummary> result = recommendationsFor(current);

        assertThat(result).extracting(GameRecommendationSummary::id)
                .containsExactly("rec-test-y", "rec-test-z");
    }

    @Test
    void ignoresDraftCandidates() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        Game draft = game("rec-test-draft", GameStatus.DRAFT);
        assign(draft, tag("action"), owner);
        assign(draft, tag("adventure"), owner);

        assertThat(recommendationsFor(current)).isEmpty();
    }

    @Test
    void countsSharedTagsOnlyOnceEvenIfSeveralUsersAssignedThem() {
        Game current = game("rec-test-current", GameStatus.PUBLISHED);
        assign(current, tag("action"), owner);
        assign(current, tag("adventure"), owner);

        User secondUser = users.save(
                new User("rec-second", "rec-second@test.com", "{bcrypt}hash"));

        Game other = game("rec-test-other", GameStatus.PUBLISHED);
        assign(other, tag("action"), owner);
        assign(other, tag("action"), secondUser);
        assign(other, tag("adventure"), secondUser);

        // "action" asignada dos veces cuenta como 1 tag compartido → 2 en
        // total, llega al umbral pero solo una vez por juego.
        assertThat(recommendationsFor(current))
                .extracting(GameRecommendationSummary::id)
                .containsExactly("rec-test-other");
    }

    private List<GameRecommendationSummary> recommendationsFor(Game game) {
        return assignments.findRecommendationsForGame(
                game.getId(),
                PageRequest.of(0, 3)
        );
    }

    private Game game(String id, GameStatus status) {
        return games.save(new Game(
                id, owner, "Juego " + id, "", null, "index.html", status));
    }

    private Tag tag(String slug) {
        return tags.findBySlug(slug)
                .orElseThrow(() -> new IllegalStateException(
                        "Tag sembrado por Flyway no encontrado: " + slug));
    }

    private void assign(Game game, Tag tag, User user) {
        assignments.save(new GameTagAssignment(game, tag, user));
    }
}
