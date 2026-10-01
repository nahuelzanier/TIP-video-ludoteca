package com.tip_video_ludoteca.comments;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

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

    protected Comment() {
    }

    public Comment(
            Game game,
            User author,
            Comment parentComment,
            String content,
            Integer rating) {
        this.game = game;
        this.author = author;
        this.parentComment = parentComment;
        this.content = content;
        this.rating = rating;
    }

    public Long getId() {
        return id;
    }

    public Game getGame() {
        return game;
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

    public boolean isRoot() {
        return parentComment == null;
    }

    public Comment rootComment() {
        return parentComment == null ? this : parentComment;
    }
}