package com.tip_video_ludoteca.config;

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

import java.util.Locale;
import java.util.Optional;

@Component
@Profile("dev")
public class DevDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    private final String seedUsername;
    private final String seedEmail;
    private final String seedPassword;

    public DevDataSeeder(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.username}") String seedUsername,
            @Value("${app.seed.email}") String seedEmail,
            @Value("${app.seed.password}") String seedPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.seedUsername = seedUsername;
        this.seedEmail = seedEmail;
        this.seedPassword = seedPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User author = findOrCreateSeedUser();
        log.info("Seed dev listo: usuario={}", author.getUsername());
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

}
