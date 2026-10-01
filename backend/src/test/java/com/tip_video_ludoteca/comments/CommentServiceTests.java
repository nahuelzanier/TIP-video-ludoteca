package com.tip_video_ludoteca.comments;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameNotFoundException;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.games.GameStatus;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentServiceTests {

    private static final String GAME_ID = "game-1";
    private static final String AUTHOR_EMAIL = "lucas2@gmail.com";
    private static final String OWNER_EMAIL = "owner@gmail.com";

    @Mock
    private CommentRepository comments;

    @Mock
    private GameRepository games;

    @Mock
    private UserRepository users;

    @InjectMocks
    private CommentService commentService;

    @Test
    void createSavesCommentWithoutRating() {
        givenGame(game(owner()));
        givenAuthor(author());
        givenSaveReturnsArgument();

        CommentResponse response = commentService.create(
                GAME_ID,
                new CreateCommentRequest("Buenísimo el salto.", null),
                AUTHOR_EMAIL);

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());

        assertThat(saved.getValue().getContent()).isEqualTo("Buenísimo el salto.");
        assertThat(saved.getValue().getRating()).isNull();
        assertThat(saved.getValue().getParentComment()).isNull();
        assertThat(response.rating()).isNull();
    }

    @Test
    void createSavesCommentWithRating() {
        givenGame(game(owner()));
        givenAuthor(author());
        givenSaveReturnsArgument();

        CommentResponse response = commentService.create(
                GAME_ID,
                new CreateCommentRequest("Impresionante.", 5),
                AUTHOR_EMAIL);

        assertThat(response.rating()).isEqualTo(5);
    }

    @Test
    void createTrimsContent() {
        givenGame(game(owner()));
        givenAuthor(author());
        givenSaveReturnsArgument();

        commentService.create(
                GAME_ID,
                new CreateCommentRequest("  Hola  ", null),
                AUTHOR_EMAIL);

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());

        assertThat(saved.getValue().getContent()).isEqualTo("Hola");
    }

    @Test
    void createRejectsBlankContent() {
        assertThatThrownBy(() -> commentService.create(
                GAME_ID,
                new CreateCommentRequest("   ", null),
                AUTHOR_EMAIL))
                .isInstanceOf(InvalidCommentException.class);

        verify(comments, never()).save(any(Comment.class));
    }

    @Test
    void createThrowsWhenGameDoesNotExist() {
        when(games.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(
                GAME_ID,
                new CreateCommentRequest("hola", null),
                AUTHOR_EMAIL))
                .isInstanceOf(GameNotFoundException.class);

        verify(comments, never()).save(any(Comment.class));
    }

    @Test
    void createThrowsWhenAuthenticatedUserDoesNotExist() {
        givenGame(game(owner()));
        when(users.findByEmailIgnoreCase(AUTHOR_EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.create(
                GAME_ID,
                new CreateCommentRequest("hola", null),
                AUTHOR_EMAIL))
                .isInstanceOf(UserNotFoundException.class);

        verify(comments, never()).save(any(Comment.class));
    }

    @Test
    void createFlagsTheGameAuthor() {
        givenGame(game(author()));
        givenAuthor(author());
        givenSaveReturnsArgument();

        CommentResponse response = commentService.create(
                GAME_ID,
                new CreateCommentRequest("Mi propio juego.", null),
                AUTHOR_EMAIL);

        assertThat(response.isGameAuthor()).isTrue();
        assertThat(response.username()).isEqualTo("lucas2");
    }

    @Test
    void createDoesNotFlagAnotherUser() {
        givenGame(game(owner()));
        givenAuthor(author());
        givenSaveReturnsArgument();

        CommentResponse response = commentService.create(
                GAME_ID,
                new CreateCommentRequest("No es mío.", null),
                AUTHOR_EMAIL);

        assertThat(response.isGameAuthor()).isFalse();
    }

    @Test
    void createReplyPointsToTheAnsweredComment() {
        givenAuthor(author());
        givenSaveReturnsArgument();
        givenFindById(comment(1L, null, game(owner()), owner(), "root", 1));

        commentService.createReply(
                1L,
                new CreateReplyRequest("Totalmente de acuerdo.", null),
                AUTHOR_EMAIL);

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());

        assertThat(saved.getValue().getParentComment().getId()).isEqualTo(1L);
        assertThat(saved.getValue().getRating()).isNull();
    }

    @Test
    void createReplyToAReplyIsFlattenedToTheRootComment() {
        Comment root = comment(1L, null, game(owner()), owner(), "root", 1);
        Comment reply = comment(2L, root, root.getGame(), owner(), "reply", 2);

        givenAuthor(author());
        givenSaveReturnsArgument();
        givenFindById(reply);

        commentService.createReply(
                2L,
                new CreateReplyRequest("Al hilo del comentario.", null),
                AUTHOR_EMAIL);

        ArgumentCaptor<Comment> saved = ArgumentCaptor.forClass(Comment.class);
        verify(comments).save(saved.capture());

        assertThat(saved.getValue().getParentComment().getId()).isEqualTo(1L);
        assertThat(saved.getValue().getGame().getId()).isEqualTo(GAME_ID);
    }

    @Test
    void createReplyRejectsRating() {
        assertThatThrownBy(() -> commentService.createReply(
                1L,
                new CreateReplyRequest("Con estrella", 4),
                AUTHOR_EMAIL))
                .isInstanceOf(InvalidCommentException.class);

        verify(comments, never()).save(any(Comment.class));
    }

    @Test
    void createReplyThrowsWhenCommentDoesNotExist() {
        when(comments.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.createReply(
                99L,
                new CreateReplyRequest("hola", null),
                AUTHOR_EMAIL))
                .isInstanceOf(CommentNotFoundException.class);

        verify(comments, never()).save(any(Comment.class));
    }

    @Test
    void listByGameNestsRepliesUnderTheirRootKeepingRootOrder() {
        Game game = game(author());

        Comment olderRoot = comment(1L, null, game, owner(), "root viejo", 1);
        Comment newerRoot = comment(2L, null, game, author(), "root nuevo", 5);
        Comment firstReply = comment(3L, newerRoot, game, owner(), "primera", 4);
        Comment secondReply = comment(4L, newerRoot, game, author(), "segunda", 5);

        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
        when(comments.findByGame_IdAndParentCommentIsNullOrderByCreatedAtDescIdDesc(GAME_ID))
                .thenReturn(List.of(newerRoot, olderRoot));
        when(comments.findByGame_IdAndParentCommentIsNotNullOrderByCreatedAtAscIdAsc(GAME_ID))
                .thenReturn(List.of(firstReply, secondReply));

        List<CommentResponse> result = commentService.listByGame(GAME_ID);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo(2L);
        assertThat(result.get(0).rating()).isEqualTo(5);
        assertThat(result.get(0).isGameAuthor()).isTrue();
        assertThat(result.get(0).replies()).hasSize(2);
        assertThat(result.get(0).replies().get(0).content()).isEqualTo("primera");
        assertThat(result.get(0).replies().get(1).content()).isEqualTo("segunda");
        assertThat(result.get(1).id()).isEqualTo(1L);
        assertThat(result.get(1).isGameAuthor()).isFalse();
        assertThat(result.get(1).replies()).isEmpty();
    }

    @Test
    void listByGameReturnsEmptyListWhenThereAreNoComments() {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game(owner())));
        when(comments.findByGame_IdAndParentCommentIsNullOrderByCreatedAtDescIdDesc(GAME_ID))
                .thenReturn(List.of());
        when(comments.findByGame_IdAndParentCommentIsNotNullOrderByCreatedAtAscIdAsc(GAME_ID))
                .thenReturn(List.of());

        assertThat(commentService.listByGame(GAME_ID)).isEmpty();
    }

    @Test
    void listByGameThrowsWhenGameDoesNotExist() {
        when(games.findById(GAME_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentService.listByGame(GAME_ID))
                .isInstanceOf(GameNotFoundException.class);
    }

    private void givenGame(Game game) {
        when(games.findById(GAME_ID)).thenReturn(Optional.of(game));
    }

    private void givenAuthor(User user) {
        when(users.findByEmailIgnoreCase(AUTHOR_EMAIL)).thenReturn(Optional.of(user));
    }

    private void givenFindById(Comment comment) {
        when(comments.findById(comment.getId())).thenReturn(Optional.of(comment));
    }

    private void givenSaveReturnsArgument() {
        when(comments.save(any(Comment.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Game game(User gameOwner) {
        return new Game(
                GAME_ID,
                gameOwner,
                "Aventura",
                "Un juego de aventura",
                "/api/games/game-1/cover",
                "index.html",
                GameStatus.PUBLISHED);
    }

    private User author() {
        return user(1L, "lucas2", AUTHOR_EMAIL);
    }

    private User owner() {
        return user(2L, "owner", OWNER_EMAIL);
    }

    private User user(Long id, String username, String email) {
        User user = new User(username, email, "{noop}encoded");
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Comment comment(
            Long id,
            Comment parent,
            Game game,
            User commentAuthor,
            String content,
            Integer rating) {

        Comment comment = new Comment(game, commentAuthor, parent, content, rating);
        ReflectionTestUtils.setField(comment, "id", id);
        ReflectionTestUtils.setField(comment, "createdAt", Instant.now().plusSeconds(id));
        return comment;
    }
}