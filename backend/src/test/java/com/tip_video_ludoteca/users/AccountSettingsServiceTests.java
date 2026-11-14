package com.tip_video_ludoteca.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountSettingsServiceTests {

    @Mock
    private UserRepository users;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AccountSettingsService service;

    @Test
    void changesUsernameOnTheExistingUserRecord() {
        User user = user();
        when(users.findByEmailIgnoreCase("player@example.test"))
                .thenReturn(Optional.of(user));
        when(users.existsByUsernameIgnoreCase("new_name")).thenReturn(false);

        var result = service.updateUsername("player@example.test", "New_Name");

        assertThat(user.getUsername()).isEqualTo("new_name");
        assertThat(result.username()).isEqualTo("new_name");
    }

    @Test
    void rejectsUsernameAlreadyUsedByAnotherAccount() {
        when(users.findByEmailIgnoreCase("player@example.test"))
                .thenReturn(Optional.of(user()));
        when(users.existsByUsernameIgnoreCase("taken"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.updateUsername("player@example.test", "taken"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void changesPasswordOnlyAfterVerifyingTheCurrentPassword() {
        User user = user();
        when(users.findByEmailIgnoreCase("player@example.test"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-password", "{noop}encoded")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("{bcrypt}new-hash");

        service.updatePassword("player@example.test", "old-password", "new-password");

        assertThat(user.getPasswordHash()).isEqualTo("{bcrypt}new-hash");
        verify(passwordEncoder).encode("new-password");
    }

    @Test
    void doesNotReplacePasswordWhenCurrentPasswordIsWrong() {
        User user = user();
        when(users.findByEmailIgnoreCase("player@example.test"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.updatePassword(
                "player@example.test", "wrong", "new-password"))
                .isInstanceOf(ResponseStatusException.class);

        verify(passwordEncoder, never()).encode(anyString());
    }

    private User user() {
        User user = new User("player", "player@example.test", "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", 12L);
        return user;
    }
}
