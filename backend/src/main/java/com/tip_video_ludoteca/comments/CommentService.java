package com.tip_video_ludoteca.comments;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameNotFoundException;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.users.User;
import com.tip_video_ludoteca.users.UserNotFoundException;
import com.tip_video_ludoteca.users.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CommentService {

    public static final int MAX_CONTENT_LENGTH = 1000;

    private static final String RATING_NOT_ALLOWED_IN_REPLY =
            "Las respuestas no pueden tener valoración. Valorá el comentario original.";

    private final CommentRepository comments;
    private final GameRepository games;
    private final UserRepository users;

    public CommentService(
            CommentRepository comments,
            GameRepository games,
            UserRepository users) {
        this.comments = comments;
        this.games = games;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByGame(String gameId) {
        Game game = findGame(gameId);

        List<Comment> roots = comments
                .findByGame_IdAndParentCommentIsNullOrderByCreatedAtDescIdDesc(gameId);

        List<Comment> replies = comments
                .findByGame_IdAndParentCommentIsNotNullOrderByCreatedAtAscIdAsc(gameId);

        Map<Long, List<CommentResponse>> repliesByParent = new LinkedHashMap<>();

        for (Comment reply : replies) {
            repliesByParent
                    .computeIfAbsent(
                            reply.getParentComment().getId(),
                            key -> new ArrayList<>())
                    .add(toResponse(reply, game));
        }

        List<CommentResponse> result = new ArrayList<>(roots.size());

        for (Comment root : roots) {
            result.add(toResponse(root, game)
                    .withReplies(repliesByParent.getOrDefault(root.getId(), List.of())));
        }

        return result;
    }

    @Transactional
    public CommentResponse create(String gameId, CreateCommentRequest request, String userEmail) {
        String content = cleanContent(request.content());

        Game game = findGame(gameId);
        User author = findUser(userEmail);

        Comment comment = new Comment(
                game,
                author,
                null,
                content,
                request.rating());

        return toResponse(comments.save(comment), game);
    }

    @Transactional
    public CommentResponse createReply(
            Long commentId,
            CreateReplyRequest request,
            String userEmail) {

        if (request.rating() != null) {
            throw new InvalidCommentException(RATING_NOT_ALLOWED_IN_REPLY);
        }

        String content = cleanContent(request.content());

        Comment target = comments.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        Comment root = target.rootComment();
        Game game = root.getGame();

        Comment reply = new Comment(
                game,
                findUser(userEmail),
                root,
                content,
                null);

        return toResponse(comments.save(reply), game);
    }

    private Game findGame(String gameId) {
        return games.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    private User findUser(String userEmail) {
        return users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);
    }

    private CommentResponse toResponse(Comment comment, Game game) {
        return CommentResponse.from(comment, game, CommentResponse.isGameAuthor(comment, game));
    }

    private String cleanContent(String content) {
        String trimmed = content.trim();

        if (trimmed.isEmpty()) {
            throw new InvalidCommentException("Escribí un comentario antes de publicarlo.");
        }

        return trimmed;
    }
}