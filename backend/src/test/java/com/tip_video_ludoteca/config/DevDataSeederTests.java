package com.tip_video_ludoteca.config;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DevDataSeederTests {

    private static final String USERNAME = "lucas2";
    private static final String EMAIL = "lucas2@gmail.com";
    private static final String PASSWORD = "lucas222";
    private static final String HASH = "{bcrypt}lucas-hash";

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DevDataSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new DevDataSeeder(users, passwordEncoder, USERNAME, EMAIL, PASSWORD);
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
    void doesNotDuplicateOrRewriteTheSeedUserOnRepeatedRuns() {
        User existing = new User(USERNAME, EMAIL, HASH);
        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(existing));

        seeder.run(null);
        seeder.run(null);

        verify(users, never()).save(any(User.class));
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void looksTheSeedUserUpIgnoringCase() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(new User(USERNAME, EMAIL, HASH)));

        seeder.run(null);

        verify(users).findByUsernameIgnoreCase(USERNAME);
        verify(users, never()).save(any(User.class));
    }
}
