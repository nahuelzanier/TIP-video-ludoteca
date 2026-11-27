package com.tip_video_ludoteca.comments;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.news.Noticia;
import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private CommentTargetType targetType = CommentTargetType.GAME;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "noticia_id")
    private Noticia noticia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private Comment parentComment;

    @Column(nullable = false, length = 1000)
    private String content;

    @Column
    private Integer rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private boolean edited = false;

    protected Comment() {
    }

    public Comment(
            Game game,
            User author,
            Comment parentComment,
            String content,
            Integer rating) {
        this(CommentTargetType.GAME, game, null, author, parentComment, content, rating);
    }

    public Comment(
            Noticia noticia,
            User author,
            Comment parentComment,
            String content,
            Integer rating) {
        this(CommentTargetType.NEWS, null, noticia, author, parentComment, content, rating);
    }

    private Comment(
            CommentTargetType targetType,
            Game game,
            Noticia noticia,
            User author,
            Comment parentComment,
            String content,
            Integer rating) {
        this.targetType = targetType;
        this.game = game;
        this.noticia = noticia;
        this.author = author;
        this.parentComment = parentComment;
        this.content = content;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public CommentTargetType getTargetType() {
        return targetType;
    }

    public Game getGame() {
        return game;
    }

    public Noticia getNoticia() {
        return noticia;
    }

    public User getAuthor() {
        return author;
    }

    public Comment getParentComment() {
        return parentComment;
    }

    public String getContent() {
        return content;
    }

    public Integer getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isEdited() {
        return edited;
    }

    public void updateContent(String content) {
        this.content = content;
        this.edited = true;
    }

    public boolean isRoot() {
        return parentComment == null;
    }

    public Comment rootComment() {
        return parentComment == null ? this : parentComment;
    }

    /**
     * Id of the author of the commented entity, used to badge "creator" replies.
     */
    public Long getTargetAuthorId() {
        if (targetType == CommentTargetType.NEWS) {
            return noticia.getAuthor().getId();
        }

        return game.getOwner() == null ? null : game.getOwner().getId();
    }
}
