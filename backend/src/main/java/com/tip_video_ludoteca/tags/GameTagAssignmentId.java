package com.tip_video_ludoteca.tags;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GameTagAssignmentId implements Serializable {

    @Column(name = "game_id", length = 36)
    private String gameId;

    @Column(name = "tag_id")
    private Long tagId;

    @Column(name = "user_id")
    private Long userId;

    protected GameTagAssignmentId() {
    }

    public GameTagAssignmentId(String gameId, Long tagId, Long userId) {
        this.gameId = gameId;
        this.tagId = tagId;
        this.userId = userId;
    }

    public String getGameId() {
        return gameId;
    }

    public Long getTagId() {
        return tagId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof GameTagAssignmentId other)) return false;

        return Objects.equals(gameId, other.gameId)
                && Objects.equals(tagId, other.tagId)
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, tagId, userId);
    }
}