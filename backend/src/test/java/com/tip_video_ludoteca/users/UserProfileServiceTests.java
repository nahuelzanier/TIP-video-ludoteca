package com.tip_video_ludoteca.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTests {

    private static final String EMAIL = "luqui@ludarium.test";
    private static final String USERNAME = "luqui";

    @Mock
    private UserRepository users;

    @InjectMocks
    private UserProfileService userProfiles;

    @Test
    void getProfileReturnsPublicData() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(user(7L, USERNAME, "Hola, diseño juegos.")));

        UserProfileResponse profile = userProfiles.getProfile(USERNAME);

        assertThat(profile.id()).isEqualTo(7L);
        assertThat(profile.username()).isEqualTo(USERNAME);
        assertThat(profile.description()).isEqualTo("Hola, diseño juegos.");
    }

    @Test
    void getProfileWithoutDescriptionReturnsNullDescription() {
        when(users.findByUsernameIgnoreCase(USERNAME))
                .thenReturn(Optional.of(user(7L, USERNAME, null)));

        assertThat(userProfiles.getProfile(USERNAME).description()).isNull();
    }

    @Test
    void getProfileFindsUserIgnoringCase() {
        when(users.findByUsernameIgnoreCase("LuQuI"))
                .thenReturn(Optional.of(user(7L, USERNAME, null)));

        assertThat(userProfiles.getProfile("LuQuI").username()).isEqualTo(USERNAME);
    }

    @Test
    void getProfileThrowsWhenUserDoesNotExist() {
        when(users.findByUsernameIgnoreCase("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfiles.getProfile("noexiste"))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateDescriptionTrimsAndSaves() {
        when(users.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user(7L, USERNAME, null)));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse profile = userProfiles.updateDescription(EMAIL, USERNAME, "  Hola  ");

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        assertThat(saved.getValue().getDescription()).isEqualTo("Hola");
        assertThat(profile.description()).isEqualTo("Hola");
    }

    @Test
    void updateDescriptionWithBlankTextClearsIt() {
        when(users.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user(7L, USERNAME, "Algo")));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(userProfiles.updateDescription(EMAIL, USERNAME, "   ").description()).isNull();
    }

    @Test
    void updateDescriptionAcceptsUsernameIgnoringCase() {
        when(users.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user(7L, USERNAME, null)));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(userProfiles.updateDescription(EMAIL, "LUQUI", "Perfil mío.").username())
                .isEqualTo(USERNAME);
    }

    @Test
    void updateDescriptionRejectsAnotherUserProfile() {
        when(users.findByEmailIgnoreCase(EMAIL))
                .thenReturn(Optional.of(user(7L, USERNAME, null)));

        assertThatThrownBy(() -> userProfiles.updateDescription(EMAIL, "otro", "hack"))
                .isInstanceOf(ProfileEditForbiddenException.class);

        verify(users, never()).save(any(User.class));
    }

    @Test
    void updateDescriptionThrowsWhenAuthenticatedUserDoesNotExist() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfiles.updateDescription(EMAIL, USERNAME, "hola"))
                .isInstanceOf(UserNotFoundException.class);
    }

    private User user(Long id, String username, String description) {
        User user = new User(username, EMAIL, "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", id);
        user.updateDescription(description);
        return user;
    }
}
