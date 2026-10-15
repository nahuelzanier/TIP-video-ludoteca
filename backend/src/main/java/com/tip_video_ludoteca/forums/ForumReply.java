package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "forum_replies")
public class ForumReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussion_id", nullable = false)
    private ForumDiscussion discussion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_reply_id")
    private ForumReply parentReply;

    @Column(nullable = false, length = 3000)
    private String content;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ForumReply() {
    }

    public ForumReply(
            ForumDiscussion discussion,
            User author,
            ForumReply parentReply,
            String content) {
        this.discussion = discussion;
        this.author = author;
        this.parentReply = parentReply;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public ForumDiscussion getDiscussion() {
        return discussion;
    }

    public User getAuthor() {
        return author;
    }

    public ForumReply getParentReply() {
        return parentReply;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}