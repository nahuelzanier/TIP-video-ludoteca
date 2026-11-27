package com.tip_video_ludoteca.comments;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.games.GameNotFoundException;
import com.tip_video_ludoteca.games.GameRepository;
import com.tip_video_ludoteca.news.Noticia;
import com.tip_video_ludoteca.news.NoticiaNotFoundException;
import com.tip_video_ludoteca.news.NoticiaRepository;
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
    private static final String RATING_NOT_ALLOWED_IN_NEWS =
            "Las noticias no pueden tener valoración.";

    private final CommentRepository comments;
    private final GameRepository games;
    private final NoticiaRepository noticias;
    private final UserRepository users;

    public CommentService(
            CommentRepository comments,
            GameRepository games,
            NoticiaRepository noticias,
            UserRepository users) {
        this.comments = comments;
        this.games = games;
        this.noticias = noticias;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByGame(String gameId) {
        Game game = findGame(gameId);
        Long targetAuthorId = game.getOwner() == null ? null : game.getOwner().getId();

        return list(
                CommentTargetType.GAME,
                gameId,
                targetAuthorId);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listByNews(Long noticiaId) {
        Noticia noticia = findNoticia(noticiaId);

        return list(
                CommentTargetType.NEWS,
                Long.toString(noticiaId),
                noticia.getAuthor().getId());
    }

    @Transactional
    public CommentResponse create(String gameId, CreateCommentRequest request, String userEmail) {
        String content = cleanContent(request.content());

        Game game = findGame(gameId);
        User author = findUser(userEmail);
        Long targetAuthorId = game.getOwner() == null ? null : game.getOwner().getId();

        Comment comment = new Comment(game, author, null, content, request.rating());

        return respond(comments.save(comment), CommentTargetType.GAME, targetAuthorId);
    }

    @Transactional
    public CommentResponse createForNews(
            Long noticiaId,
            CreateCommentRequest request,
            String userEmail) {

        if (request.rating() != null) {
            throw new InvalidCommentException(RATING_NOT_ALLOWED_IN_NEWS);
        }

        String content = cleanContent(request.content());

        Noticia noticia = findNoticia(noticiaId);
        User author = findUser(userEmail);

        Comment comment = new Comment(noticia, author, null, content, null);

        return respond(
                comments.save(comment),
                CommentTargetType.NEWS,
                noticia.getAuthor().getId());
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
        User author = findUser(userEmail);
        CommentTargetType targetType = root.getTargetType();
        Long targetAuthorId = root.getTargetAuthorId();

        Comment reply = switch (targetType) {
            case GAME -> new Comment(root.getGame(), author, root, content, null);
            case NEWS -> new Comment(root.getNoticia(), author, root, content, null);
        };

        return respond(comments.save(reply), targetType, targetAuthorId);
    }

    @Transactional
    public CommentResponse update(
            Long commentId,
            UpdateCommentRequest request,
            String userEmail) {

        Comment comment = comments.findById(commentId)
                .orElseThrow(CommentNotFoundException::new);

        User authenticated = findUser(userEmail);

        if (!comment.getAuthor().getId().equals(authenticated.getId())) {
            throw new CommentEditForbiddenException();
        }

        comment.updateContent(cleanContent(request.content()));

        return respond(
                comments.save(comment),
                comment.getTargetType(),
                comment.getTargetAuthorId());
    }

    private List<CommentResponse> list(
            CommentTargetType targetType,
            String targetId,
            Long targetAuthorId) {

        List<Comment> roots;
        List<Comment> replies;

        if (targetType == CommentTargetType.GAME) {
            roots = comments
                    .findByGame_IdAndParentCommentIsNullOrderByCreatedAtDescIdDesc(targetId);
            replies = comments
                    .findByGame_IdAndParentCommentIsNotNullOrderByCreatedAtAscIdAsc(targetId);
        } else {
            Long noticiaId = Long.valueOf(targetId);
            roots = comments
                    .findByNoticia_IdAndParentCommentIsNullOrderByCreatedAtDescIdDesc(noticiaId);
            replies = comments
                    .findByNoticia_IdAndParentCommentIsNotNullOrderByCreatedAtAscIdAsc(noticiaId);
        }

        Map<Long, List<CommentResponse>> repliesByParent = new LinkedHashMap<>();

        for (Comment reply : replies) {
            repliesByParent
                    .computeIfAbsent(
                            reply.getParentComment().getId(),
                            key -> new ArrayList<>())
                    .add(respond(reply, targetType, targetAuthorId));
        }

        List<CommentResponse> result = new ArrayList<>(roots.size());

        for (Comment root : roots) {
            result.add(respond(root, targetType, targetAuthorId)
                    .withReplies(repliesByParent.getOrDefault(root.getId(), List.of())));
        }

        return result;
    }

    private CommentResponse respond(
            Comment comment,
            CommentTargetType targetType,
            Long targetAuthorId) {

        boolean isTargetAuthor = targetAuthorId != null
                && comment.getAuthor().getId().equals(targetAuthorId);

        return CommentResponse.from(
                comment,
                targetType == CommentTargetType.GAME && isTargetAuthor,
                isTargetAuthor);
    }

    private Game findGame(String gameId) {
        return games.findById(gameId).orElseThrow(GameNotFoundException::new);
    }

    private Noticia findNoticia(Long noticiaId) {
        return noticias.findById(noticiaId).orElseThrow(NoticiaNotFoundException::new);
    }

    private User findUser(String userEmail) {
        return users.findByEmailIgnoreCase(userEmail)
                .orElseThrow(UserNotFoundException::new);
    }

    private String cleanContent(String content) {
        String trimmed = content.trim();

        if (trimmed.isEmpty()) {
            throw new InvalidCommentException("Escribí un comentario antes de publicarlo.");
        }

        return trimmed;
    }
}
