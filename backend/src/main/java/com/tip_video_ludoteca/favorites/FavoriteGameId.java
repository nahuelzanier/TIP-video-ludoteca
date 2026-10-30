package com.tip_video_ludoteca.favorites;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FavoriteGameId implements Serializable {

    private Long userId;
    private String gameId;

    protected FavoriteGameId() {
    }

    public FavoriteGameId(Long userId, String gameId) {
        this.userId = userId;
        this.gameId = gameId;
    }

    public Long getUserId() {
        return userId;
    }

    public String getGameId() {
        return gameId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FavoriteGameId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId)
                && Objects.equals(gameId, that.gameId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, gameId);
    }
}
