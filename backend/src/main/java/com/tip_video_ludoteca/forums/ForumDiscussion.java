package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "forum_discussions")
public class ForumDiscussion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "cover_image", columnDefinition = "bytea")
    private byte[] coverImage;

    @Column(name = "cover_image_content_type", length = 50)
    private String coverImageContentType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "forum_discussion_forum_tags",
            joinColumns = @JoinColumn(name = "discussion_id"),
            inverseJoinColumns = @JoinColumn(name = "forum_tag_id")
    )
    private Set<ForumTag> tags = new HashSet<>();

    protected ForumDiscussion() {
    }

    public ForumDiscussion(
            Game game,
            User author,
            String title,
            String description,
            byte[] coverImage,
            String coverImageContentType) {
        this.game = game;
        this.author = author;
        this.title = title;
        this.description = description;
        this.coverImage = coverImage;
        this.coverImageContentType = coverImageContentType;
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

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public byte[] getCoverImage() {
        return coverImage;
    }

    public String getCoverImageContentType() {
        return coverImageContentType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Set<ForumTag> getTags() {
        return tags;
    }
}