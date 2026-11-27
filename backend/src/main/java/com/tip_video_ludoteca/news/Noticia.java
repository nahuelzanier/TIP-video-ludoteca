package com.tip_video_ludoteca.news;

import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "noticias")
public class Noticia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, length = 3000)
    private String description;

    @Column(name = "published_at", nullable = false, updatable = false)
    private Instant publishedAt = Instant.now();

    @Column(name = "has_image", nullable = false)
    private boolean hasImage = false;

    @OneToOne(mappedBy = "noticia", fetch = FetchType.LAZY)
    private NoticiaImage image;

    protected Noticia() {
    }

    public Noticia(String title, String description, User author) {
        this.title = title;
        this.description = description;
        this.author = author;
    }

    public Long getId() {
        return id;
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

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public NoticiaImage getImage() {
        return image;
    }

    public void update(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public void markWithImage() {
        this.hasImage = true;
    }

    public void markWithoutImage() {
        this.hasImage = false;
    }

    public boolean isWrittenBy(User user) {
        return author.getId().equals(user.getId());
    }
}
