package com.tip_video_ludoteca.games;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class GameFileId implements Serializable {

    @Column(name = "game_id", nullable = false, length = 36)
    private String gameId;

    @Column(name = "path", nullable = false, length = 500)
    private String path;

    protected GameFileId() {
    }

    public GameFileId(String gameId, String path) {
        this.gameId = gameId;
        this.path = path;
    }

    public String getGameId() {
        return gameId;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof GameFileId other)) return false;
        return Objects.equals(gameId, other.gameId)
                && Objects.equals(path, other.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gameId, path);
    }
}