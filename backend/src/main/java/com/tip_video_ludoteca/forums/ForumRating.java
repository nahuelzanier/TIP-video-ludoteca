package com.tip_video_ludoteca.forums;

import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "forum_ratings")
public class ForumRating {

    @EmbeddedId
    private ForumRatingId id;

    @MapsId("discussionId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "discussion_id", nullable = false)
    private ForumDiscussion discussion;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, columnDefinition = "smallint")
    private Short rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ForumRating() {
    }

    public ForumRating(ForumDiscussion discussion, User user, int rating) {
        this.id = new ForumRatingId(discussion.getId(), user.getId());
        this.discussion = discussion;
        this.user = user;
        updateRating(rating);
        this.createdAt = Instant.now();
    }

    public ForumRatingId getId() {
        return id;
    }

    public ForumDiscussion getDiscussion() {
        return discussion;
    }

    public User getUser() {
        return user;
    }

    public Short getRating() {
        return rating;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateRating(int rating) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5.");
        }

        this.rating = (short) rating;
        this.updatedAt = Instant.now();
    }
}