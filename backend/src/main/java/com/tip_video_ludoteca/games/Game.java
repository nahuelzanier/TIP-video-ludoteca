package com.tip_video_ludoteca.games;

import jakarta.persistence.*;

@Entity
@Table(name = "game")
public class Game {

    @Id
    @Column(name = "id", nullable = false, length = 50)
    private String id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 255)
    private String image;

    @Column(nullable = false, length = 500)
    private String description;

    protected Game() {
    }

    public Game(String id, String title, String image, String description) {
        this.id = id;
        this.title = title;
        this.image = image;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getImage() {
        return image;
    }

    public String getDescription() {
        return description;
    }
}
