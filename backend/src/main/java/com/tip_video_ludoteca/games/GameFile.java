package com.tip_video_ludoteca.games;

import jakarta.persistence.*;

@Entity
@Table(name = "game_files")
public class GameFile {

    @EmbeddedId
    private GameFileId id;

    @MapsId("gameId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "content_type", nullable = false, length = 150)
    private String contentType;

    @Column(name = "content", nullable = false, columnDefinition = "bytea")
    private byte[] content;

    protected GameFile() {
    }

    public GameFile(Game game, String path, String contentType, byte[] content) {
        this.id = new GameFileId(game.getId(), path);
        this.game = game;
        this.contentType = contentType;
        this.content = content;
    }

    public GameFileId getId() {
        return id;
    }

    public Game getGame() {
        return game;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }
}