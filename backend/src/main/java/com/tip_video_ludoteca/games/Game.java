package com.tip_video_ludoteca.games;

import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "games")
public class Game {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String description = "";

    @Column(name = "cover_image_url", length = 500)
    private String coverImageUrl;

    @Column(name = "entry_file", nullable = false, length = 500)
    private String entryFile = "index.html";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GameStatus status = GameStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Game() {
    }

    public Game(
            String id,
            User owner,
            String title,
            String description,
            String coverImageUrl,
            String entryFile,
            GameStatus status) {
        this.id = id;
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.coverImageUrl = coverImageUrl;
        this.entryFile = entryFile;
        this.status = status;
    }

    public String getId() {
        return id;
    }

    public User getOwner() {
        return owner;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public String getEntryFile() {
        return entryFile;
    }

    public GameStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}