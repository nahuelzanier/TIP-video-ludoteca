package com.tip_video_ludoteca.users;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileImageServiceTests {

    private static final String EMAIL = "luqui@ludarium.test";
    private static final String USERNAME = "luqui";

    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x01, 0x02
    };

    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x10, 0x20
    };

    @Mock
    private UserRepository users;

    @InjectMocks
    private UserProfileImageService profileImages;

    @Test
    void updateAvatarStoresPngAndDerivesTheUrl() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse profile = profileImages.updateAvatar(EMAIL, USERNAME, file(PNG));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        assertThat(saved.getValue().getProfileImage()).isEqualTo(PNG);
        assertThat(saved.getValue().getProfileImageContentType()).isEqualTo("image/png");
        assertThat(profile.avatarUrl()).isEqualTo("/api/users/luqui/avatar");
    }

    @Test
    void updateAvatarStoresJpeg() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        profileImages.updateAvatar(EMAIL, USERNAME, file(JPEG));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        assertThat(saved.getValue().getProfileImageContentType()).isEqualTo("image/jpeg");
    }

    @Test
    void updateAvatarReplacesThePreviousImage() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        profileImages.updateAvatar(EMAIL, USERNAME, file(PNG));
        profileImages.updateAvatar(EMAIL, USERNAME, file(JPEG));

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users, org.mockito.Mockito.times(2)).save(saved.capture());

        assertThat(saved.getAllValues().get(1).getProfileImage()).isEqualTo(JPEG);
    }

    @Test
    void updateAvatarRejectsAnEmptyFile() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> profileImages.updateAvatar(EMAIL, USERNAME, null))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(thrown -> assertThat(((ResponseStatusException) thrown).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));

        verify(users, never()).save(any(User.class));
    }

    @Test
    void updateAvatarRejectsAFileOverTwoMegabytes() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        MultipartFile tooBig = new MockMultipartFile(
                "avatar", "foto.png", "image/png", new byte[2 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> profileImages.updateAvatar(EMAIL, USERNAME, tooBig))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("2 MB");

        verify(users, never()).save(any(User.class));
    }

    @Test
    void updateAvatarRejectsBytesThatAreNotPngOrJpeg() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        MultipartFile fake = new MockMultipartFile(
                "avatar", "foto.png", "image/png", "<svg onload=alert(1)>".getBytes());

        assertThatThrownBy(() -> profileImages.updateAvatar(EMAIL, USERNAME, fake))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PNG o JPEG");

        verify(users, never()).save(any(User.class));
    }

    @Test
    void updateAvatarIgnoresTheDeclaredContentType() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MultipartFile mislabelled = new MockMultipartFile(
                "avatar", "foto.png", "text/html", PNG);

        profileImages.updateAvatar(EMAIL, USERNAME, mislabelled);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        assertThat(saved.getValue().getProfileImageContentType()).isEqualTo("image/png");
    }

    @Test
    void updateAvatarRejectsAnotherUserProfile() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> profileImages.updateAvatar(EMAIL, "otro", file(PNG)))
                .isInstanceOf(ProfileEditForbiddenException.class);

        verify(users, never()).save(any(User.class));
    }

    @Test
    void updateAvatarAcceptsTheUsernameIgnoringCase() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(profileImages.updateAvatar(EMAIL, "LUQUI", file(PNG)).username())
                .isEqualTo(USERNAME);
    }

    @Test
    void updateAvatarThrowsWhenAuthenticatedUserDoesNotExist() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileImages.updateAvatar(EMAIL, USERNAME, file(PNG)))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void removeAvatarClearsTheStoredImage() {
        User user = user();
        user.updateProfileImage(PNG, "image/png");

        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user));
        when(users.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserProfileResponse profile = profileImages.removeAvatar(EMAIL, USERNAME);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(users).save(saved.capture());

        assertThat(saved.getValue().hasProfileImage()).isFalse();
        assertThat(saved.getValue().getProfileImageContentType()).isNull();
        assertThat(profile.avatarUrl()).isNull();
    }

    @Test
    void removeAvatarRejectsAnotherUserProfile() {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> profileImages.removeAvatar(EMAIL, "otro"))
                .isInstanceOf(ProfileEditForbiddenException.class);

        verify(users, never()).save(any(User.class));
    }

    @Test
    void readAvatarReturnsTheBytesWithTheStoredContentType() {
        User user = user();
        user.updateProfileImage(PNG, "image/png");

        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user));

        ResponseEntity<byte[]> response = profileImages.readAvatar(USERNAME);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getBody()).isEqualTo(PNG);
    }

    @Test
    void readAvatarReturnsNotFoundWhenTheUserHasNoImage() {
        when(users.findByUsernameIgnoreCase(USERNAME)).thenReturn(Optional.of(user()));

        assertThat(profileImages.readAvatar(USERNAME).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void readAvatarReturnsNotFoundForAnUnknownUser() {
        when(users.findByUsernameIgnoreCase("noexiste")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileImages.readAvatar("noexiste"))
                .isInstanceOf(UserNotFoundException.class);
    }

    private MultipartFile file(byte[] content) {
        return new MockMultipartFile("avatar", "foto.png", "image/png", content);
    }

    private User user() {
        User user = new User(USERNAME, EMAIL, "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", 7L);
        return user;
    }
}