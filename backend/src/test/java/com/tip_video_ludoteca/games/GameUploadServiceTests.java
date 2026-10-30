package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameUploadServiceTests {

    private static final String EMAIL = "creator@ludarium.test";
    private static final byte[] PNG = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @Mock
    private GameRepository games;

    @Mock
    private GameFileRepository gameFiles;

    @Mock
    private UserRepository users;

    @InjectMocks
    private GameUploadService uploads;

    @Test
    void uploadStoresDraftAndArchiveFilesAlongWithValidatedCover() throws IOException {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));
        when(games.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(gameFiles.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        GameUploadService.UploadResult result = uploads.upload(
                "  Tiny Quest  ",
                "  A small adventure.  ",
                file("cover.png", PNG),
                archive("index.html", "<h1>Game</h1>", "assets/main.js", "start();"),
                EMAIL);

        ArgumentCaptor<Game> gameCaptor = ArgumentCaptor.forClass(Game.class);
        verify(games).save(gameCaptor.capture());
        Game savedGame = gameCaptor.getValue();

        assertThat(result.id()).isEqualTo(savedGame.getId());
        assertThat(result.title()).isEqualTo("Tiny Quest");
        assertThat(result.filesStored()).isEqualTo(3);
        assertThat(savedGame.getStatus()).isEqualTo(GameStatus.DRAFT);
        assertThat(savedGame.getDescription()).isEqualTo("A small adventure.");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GameFile>> filesCaptor = ArgumentCaptor.forClass(List.class);
        verify(gameFiles).saveAll(filesCaptor.capture());
        assertThat(filesCaptor.getValue())
                .extracting(file -> file.getId().getPath())
                .containsExactlyInAnyOrder(
                        "index.html", "assets/main.js", "__metadata__/thumbnail");
        assertThat(filesCaptor.getValue())
                .filteredOn(file -> file.getId().getPath().equals("__metadata__/thumbnail"))
                .singleElement()
                .satisfies(file -> assertThat(file.getContentType()).isEqualTo("image/png"));
    }

    @Test
    void uploadRejectsArchiveWithoutRootIndexHtml() throws IOException {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> uploads.upload(
                "Game", "", file("cover.png", PNG), archive("folder/index.html", "game"), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST))
                .hasMessageContaining("index.html at its root");

        verify(games, never()).save(any(Game.class));
        verify(gameFiles, never()).saveAll(any());
    }

    @Test
    void uploadRejectsZipPathTraversal() throws IOException {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> uploads.upload(
                "Game", "", file("cover.png", PNG), archive("../outside.txt", "unsafe"), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("invalid file path");

        verify(games, never()).save(any(Game.class));
        verify(gameFiles, never()).saveAll(any());
    }

    @Test
    void uploadRejectsDuplicateArchivePaths() throws IOException {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> uploads.upload(
                "Game", "", file("cover.png", PNG),
                duplicateArchive(), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("duplicate file path");

        verify(games, never()).save(any(Game.class));
    }

    @Test
    void uploadRejectsArchiveThatUsesTheReservedCoverPath() throws IOException {
        when(users.findByEmailIgnoreCase(EMAIL)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> uploads.upload(
                "Game", "", file("cover.png", PNG),
                archive("index.html", "game", "__metadata__/thumbnail", "forged"), EMAIL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("reserved path");

        verify(games, never()).save(any(Game.class));
    }

    private MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "application/octet-stream", bytes);
    }

    private MockMultipartFile archive(String... pathAndContents) throws IOException {
        return new MockMultipartFile(
                "archive", "game.zip", "application/zip", zipBytes(pathAndContents));
    }

    private MockMultipartFile duplicateArchive() throws IOException {
        byte[] content = zipBytes("index.html", "first", "scripts.js", "second");
        byte[] original = "scripts.js".getBytes();
        byte[] duplicate = "index.html".getBytes();

        for (int index = 0; index <= content.length - original.length; index++) {
            boolean matches = true;
            for (int offset = 0; offset < original.length; offset++) {
                if (content[index + offset] != original[offset]) {
                    matches = false;
                    break;
                }
            }

            if (matches) {
                System.arraycopy(duplicate, 0, content, index, duplicate.length);
                index += original.length - 1;
            }
        }

        return new MockMultipartFile("archive", "game.zip", "application/zip", content);
    }

    private byte[] zipBytes(String... pathAndContents) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int index = 0; index < pathAndContents.length; index += 2) {
                zip.putNextEntry(new ZipEntry(pathAndContents[index]));
                zip.write(pathAndContents[index + 1].getBytes());
                zip.closeEntry();
            }
        }

        return bytes.toByteArray();
    }

    private User user() {
        User user = new User("creator", EMAIL, "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", 3L);
        return user;
    }
}
