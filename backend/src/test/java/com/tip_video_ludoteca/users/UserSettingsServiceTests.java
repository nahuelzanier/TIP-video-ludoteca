package com.tip_video_ludoteca.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserSettingsServiceTests {

    @Mock
    private UserRepository users;

    @Mock
    private UserSettingsRepository settings;

    @InjectMocks
    private UserSettingsService service;

    @Test
    void returnsDefaultsWhenTheUserHasNotSavedPreferencesYet() {
        User user = user();
        when(users.findByEmailIgnoreCase("player@example.test"))
                .thenReturn(Optional.of(user));
        when(settings.findById(12L)).thenReturn(Optional.empty());

        assertThat(service.get("player@example.test"))
                .isEqualTo(new UserSettingsService.SettingsResponse(100, "en", "system"));
    }

    @Test
    void savesOnlyTheUsersPreferenceValues() {
        User user = user();
        when(users.findByEmailIgnoreCase("player@example.test"))
                .thenReturn(Optional.of(user));
        when(settings.findById(12L)).thenReturn(Optional.empty());
        when(settings.save(org.mockito.ArgumentMatchers.any(UserSettings.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.update("player@example.test",
                new UserSettingsService.UpdateSettingsRequest(65, "es", "dark"));

        assertThat(result).isEqualTo(new UserSettingsService.SettingsResponse(65, "es", "dark"));
        verify(settings).save(org.mockito.ArgumentMatchers.argThat(value ->
                value.getUserId().equals(12L)
                        && value.getVolume() == 65
                        && value.getLanguage().equals("es")
                        && value.getTheme().equals("dark")));
    }

    private User user() {
        User user = new User("player", "player@example.test", "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", 12L);
        return user;
    }
}
