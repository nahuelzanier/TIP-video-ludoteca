package com.tip_video_ludoteca.news;

import jakarta.persistence.*;

@Entity
@Table(name = "noticia_images")
public class NoticiaImage {

    @Id
    @Column(name = "noticia_id")
    private Long noticiaId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "noticia_id", nullable = false)
    private Noticia noticia;

    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] content;

    protected NoticiaImage() {
    }

    public NoticiaImage(Noticia noticia, String contentType, byte[] content) {
        this.noticia = noticia;
        this.contentType = contentType;
        this.content = content;
    }

    public Long getNoticiaId() {
        return noticiaId;
    }

    public Noticia getNoticia() {
        return noticia;
    }

    public String getContentType() {
        return contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void replaceContent(String contentType, byte[] content) {
        this.contentType = contentType;
        this.content = content;
    }
}
