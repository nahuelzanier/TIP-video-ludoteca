package com.tip_video_ludoteca.forums;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ForumRatingId implements Serializable {

    @Column(name = "discussion_id")
    private Long discussionId;

    @Column(name = "user_id")
    private Long userId;

    protected ForumRatingId() {
    }

    public ForumRatingId(Long discussionId, Long userId) {
        this.discussionId = discussionId;
        this.userId = userId;
    }

    public Long getDiscussionId() {
        return discussionId;
    }

    public Long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ForumRatingId other)) return false;

        return Objects.equals(discussionId, other.discussionId)
                && Objects.equals(userId, other.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(discussionId, userId);
    }
}