package com.tip_video_ludoteca.config;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DevDataSeederRepositoryTests {

    private static final String SEED_USERNAME = "seed-account";
    private static final String SEED_EMAIL = "seed-account@test.com";
    private static final String SEED_PASSWORD = "test-password";

    @Autowired
    private UserRepository users;

    @Autowired
    private GameRepository games;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @Test
    void repeatedSeedRunsLeaveExistingGameOwnersAndStatusesUnchanged() {
        User draftOwner = users.saveAndFlush(
                new User("draft-owner", "draft-owner@test.com", "{noop}hash"));
        User publishedOwner = users.saveAndFlush(
                new User("published-owner", "published-owner@test.com", "{noop}hash"));
        Game draft = games.saveAndFlush(game("existing-draft", draftOwner, GameStatus.DRAFT));
        Game published = games.saveAndFlush(
                game("existing-published", publishedOwner, GameStatus.PUBLISHED));

        given(passwordEncoder.encode(SEED_PASSWORD)).willReturn("{noop}seed-hash");
        DevDataSeeder seeder = new DevDataSeeder(
                users,
                passwordEncoder,
                SEED_USERNAME,
                SEED_EMAIL,
                SEED_PASSWORD);

        seeder.run(null);
        seeder.run(null);

        entityManager.flush();
        entityManager.clear();

        Game unchangedDraft = games.findById(draft.getId()).orElseThrow();
        Game unchangedPublished = games.findById(published.getId()).orElseThrow();

        assertThat(unchangedDraft.getOwner().getId()).isEqualTo(draftOwner.getId());
        assertThat(unchangedDraft.getStatus()).isEqualTo(GameStatus.DRAFT);
        assertThat(unchangedPublished.getOwner().getId()).isEqualTo(publishedOwner.getId());
        assertThat(unchangedPublished.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        assertThat(users.existsByUsernameIgnoreCase(SEED_USERNAME)).isTrue();
    }

    private Game game(String id, User owner, GameStatus status) {
        return new Game(id, owner, "Game " + id, "", null, "index.html", status);
    }
}
