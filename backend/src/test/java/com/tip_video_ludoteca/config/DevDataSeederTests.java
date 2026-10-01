package com.tip_video_ludoteca.config;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DevDataSeederTests {

    private static final String USERNAME = "lucas2";
    private static final String EMAIL = "lucas2@gmail.com";
    private static final String PASSWORD = "lucas222";
    private static final String HASH = "{bcrypt}lucas-hash";
    private static final long SEED_USER_ID = 7L;

    @Mock
    private UserRepository users;

    @Mock
    private GameRepository games;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DevDataSeeder(users, games, passwordEncoder, USERNAME, EMAIL, PASSWORD);

        when(games.findByOwner_IdNot(SEED_USER_ID)).thenReturn(List.of());
    }

    @Test
    void createsTheSeedUserWithAHashedPassword() {
        when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
        when(users.save(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.empty());

        seeder.run(null);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        assertThat(saved.getValue().getUsername()).isEqualTo(USERNAME);
        assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
        assertThat(saved.getValue().getPasswordHash()).isEqualTo(HASH);
        assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(PASSWORD);

        verify(passwordEncoder).encode(PASSWORD);
    }

    @Test
    void doesNotDuplicateTheSeedUserOnASecondRun() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(seedUser()));

        seeder.run(null);
        seeder.run(null);

        verify(users, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void looksTheSeedUserUpIgnoringCase() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(seedUser()));

        seeder.run(null);

        verify(users).findByUsernameIgnoreCase(USERNAME);
        verify(users, never()).save(any(User.class));
    }

    @Test
    void assignsTheSeedUserAsAuthorOfTheExistingGames() {
        User seedUser = seedUser();
        Game otherGame = new Game(
                "game-1",
                seedUser,
                "Aventura",
                "",
                null,
                "index.html",
                GameStatus.PUBLISHED);

        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(seedUser));
        when(games.findByOwner_IdNot(SEED_USER_ID)).thenReturn(List.of(otherGame));

        seeder.run(null);

        verify(games).saveAll(List.of(otherGame));
        assertThat(otherGame.getOwner().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void leavesGamesAlreadyOwnedByTheSeedUserUntouched() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(seedUser()));

        seeder.run(null);

        verify(games, never()).saveAll(any());
    }

    @Test
    void publishesDraftGamesSoTheyShowUpOnTheHomePage() {
        User seedUser = seedUser();
        Game draft = new Game(
                "game-2",
                seedUser,
                "Borrador",
                "",
                null,
                "index.html",
                GameStatus.DRAFT);

        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(seedUser));
        when(games.findByStatusOrderByCreatedAtDesc(GameStatus.DRAFT))
                .thenReturn(List.of(draft));

        seeder.run(null);

        assertThat(draft.getStatus()).isEqualTo(GameStatus.PUBLISHED);
        verify(games).saveAll(List.of(draft));
    }

    @Test
    void doesNotTouchGamesThatAreAlreadyPublished() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(seedUser()));
        when(games.findByStatusOrderByCreatedAtDesc(GameStatus.DRAFT))
                .thenReturn(List.of());

        seeder.run(null);

        verify(games, never()).saveAll(any());
    }

    private User seedUser() {
        User user = new User(USERNAME, EMAIL, HASH);
        ReflectionTestUtils.setField(user, "id", SEED_USER_ID);
        return user;
    }
}