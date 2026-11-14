package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class GameCustomizationRepositoryTests {

    @Autowired private GameCustomizationRepository customizations;
    @Autowired private GameRepository games;
    @Autowired private UserRepository users;
    @Autowired private JdbcTemplate jdbc;

    @Test
    void storesAppearanceByGameIdAndDeletesItWhenTheGameIsDeleted() {
        String suffix = UUID.randomUUID().toString();
        String gameId = UUID.randomUUID().toString();
        User owner = users.saveAndFlush(new User(
                "appearance_" + suffix.substring(0, 8),
                "appearance_" + suffix + "@test.com",
                "{noop}test"));
        Game game = games.saveAndFlush(new Game(
                gameId, owner, "Appearance test", "", null, "index.html", GameStatus.PUBLISHED));

        customizations.saveAndFlush(new GameCustomization(
                game.getId(), "#102030", "#d080ff", "#eeeeee", "GEORGIA", "GRADIENT"));

        assertThat(customizations.findByGameId(gameId))
                .get()
                .extracting(GameCustomization::getFontFamily)
                .isEqualTo("GEORGIA");
        assertThat(jdbc.queryForObject(
                "SELECT game_id FROM game_customizations WHERE game_id = ?", String.class, gameId))
                .isEqualTo(gameId);

        games.deleteById(gameId);
        games.flush();

        assertThat(customizations.existsById(gameId)).isFalse();
    }
}
