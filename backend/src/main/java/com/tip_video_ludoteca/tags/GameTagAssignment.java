package com.tip_video_ludoteca.tags;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "game_tag_assignments")
public class GameTagAssignment {

    @EmbeddedId
    private GameTagAssignmentId id;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @MapsId("tagId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tag_id", nullable = false)
    private Tag tag;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    private Instant assignedAt;

    protected GameTagAssignment() {
    }

    public GameTagAssignment(Game game, Tag tag, User user) {
        this.id = new GameTagAssignmentId(
                game.getId(),
                tag.getId(),
                user.getId()
        );
        this.game = game;
        this.tag = tag;
        this.user = user;
        this.assignedAt = Instant.now();
    }

    public GameTagAssignmentId getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public Tag getTag() {
        return tag;
    }

    public User getUser() {
        return user;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }
}