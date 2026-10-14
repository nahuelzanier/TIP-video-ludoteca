package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameRepositoryTests {

    @Autowired
    private GameRepository games;

    @Autowired
    private UserRepository users;

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

        Optional<Game> result = games.findRandomByStatus("PUBLISHED");

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

    private Game game(String id, User owner, GameStatus status) {
        return new Game(id, owner, "Juego " + id, "", null, "index.html", status);
    }
}
