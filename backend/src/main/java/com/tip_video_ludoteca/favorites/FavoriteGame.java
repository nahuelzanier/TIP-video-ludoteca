package com.tip_video_ludoteca.favorites;

import com.tip_video_ludoteca.games.Game;
import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "favorite_games")
public class FavoriteGame {

    @EmbeddedId
    private FavoriteGameId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected FavoriteGame() {
    }

    public FavoriteGame(User user, Game game) {
        this.id = new FavoriteGameId(user.getId(), game.getId());
        this.user = user;
        this.game = game;
    }

    public FavoriteGameId getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Game getGame() {
        return game;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
