package com.tip_video_ludoteca.controller;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameFile;
import com.tip_video_ludoteca.games.GameFileRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTests {

    private static final String GAME_ID = "game-1";
    private static final byte[] CONTENT = "<h1>Game</h1>".getBytes();

    @Mock
    private GameFileRepository gameFiles;

    @InjectMocks
    private GameController controller;

    @Test
    void returnsStoredGameContentWithItsValidatedContentType() {
        GameFile file = file("index.html", "text/html; charset=UTF-8");
        when(gameFiles.findPublishedByGameIdAndPath(GAME_ID, "index.html"))
                .thenReturn(Optional.of(file));
        MockHttpServletRequest request = request("/games/game-1/index.html");

        var response = controller.getGameFile(GAME_ID, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType())
                .isEqualTo(MediaType.parseMediaType("text/html; charset=UTF-8"));
        assertThat(response.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(response.getBody()).isEqualTo(CONTENT);
    }

    @Test
    void rejectsTraversalPathsBeforeLookingUpFiles() {
        var response = controller.getGameFile(
                GAME_ID,
                request("/games/game-1/../../private.txt"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(gameFiles, never()).findPublishedByGameIdAndPath(GAME_ID, "../private.txt");
    }

    @Test
    void returnsNotFoundForAFileThatIsNotStored() {
        when(gameFiles.findPublishedByGameIdAndPath(GAME_ID, "missing.js"))
                .thenReturn(Optional.empty());

        var response = controller.getGameFile(
                GAME_ID,
                request("/games/game-1/missing.js"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void doesNotReturnFilesForDraftGames() {
        when(gameFiles.findPublishedByGameIdAndPath(GAME_ID, "index.html"))
                .thenReturn(Optional.empty());

        var response = controller.getGameFile(
                GAME_ID,
                request("/games/game-1/index.html"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(gameFiles).findPublishedByGameIdAndPath(GAME_ID, "index.html");
    }

    private MockHttpServletRequest request(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(requestUri);
        return request;
    }

    private GameFile file(String path, String contentType) {
        User owner = new User("owner", "owner@ludarium.test", "{noop}encoded");
        Game game = new Game(
                GAME_ID, owner, "Game", "", null, "index.html", GameStatus.PUBLISHED);
        GameFile file = new GameFile(game, path, contentType, CONTENT);
        ReflectionTestUtils.setField(game, "id", GAME_ID);
        return file;
    }
}
