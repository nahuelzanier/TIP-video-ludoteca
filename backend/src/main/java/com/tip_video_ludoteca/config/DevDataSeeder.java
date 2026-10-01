package com.tip_video_ludoteca.config;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final GameRepository games;
    private final PasswordEncoder passwordEncoder;

    private final String seedUsername;
    private final String seedEmail;
    private final String seedPassword;

    public DevDataSeeder(
            UserRepository users,
            GameRepository games,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.username}") String seedUsername,
            @Value("${app.seed.email}") String seedEmail,
            @Value("${app.seed.password}") String seedPassword) {
        this.users = users;
        this.games = games;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = seedUsername;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User author = findOrCreateSeedUser();
        int reassigned = assignAuthorToExistingGames(author);
        int published = publishDraftGames();

        log.info("Seed dev listo: usuario={} juegos reasignados={} juegos publicados={}",
                author.getUsername(), reassigned, published);
    }

    private User findOrCreateSeedUser() {
        String username = seedUsername.trim().toLowerCase(Locale.ROOT);

        Optional<User> existing = users.findByUsernameIgnoreCase(username);

        if (existing.isPresent()) {
            return existing.get();
        }

        return users.save(new User(
                username,
                seedEmail.trim().toLowerCase(Locale.ROOT),
                passwordEncoder.encode(seedPassword)));
    }

    private int assignAuthorToExistingGames(User author) {
        List<Game> otherGames = games.findByOwner_IdNot(author.getId());

        if (otherGames.isEmpty()) {
            return 0;
        }

        for (Game game : otherGames) {
            game.assignOwner(author);
        }

        games.saveAll(otherGames);

        return otherGames.size();
    }

    private int publishDraftGames() {
        List<Game> drafts = games.findByStatusOrderByCreatedAtDesc(GameStatus.DRAFT);

        if (drafts.isEmpty()) {
            return 0;
        }

        for (Game game : drafts) {
            game.publish();
        }

        games.saveAll(drafts);

        return drafts.size();
    }
}