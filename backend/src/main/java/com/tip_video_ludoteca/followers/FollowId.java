package com.tip_video_ludoteca.followers;

import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class FollowId implements Serializable {

    private Long followerId;
    private Long followedId;

    protected FollowId() {
    }

    public FollowId(Long followerId, Long followedId) {
        this.followerId = followerId;
        this.followedId = followedId;
    }

    public Long getFollowerId() {
        return followerId;
    }

    public Long getFollowedId() {
        return followedId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof FollowId that)) {
            return false;
        }
        return Objects.equals(followerId, that.followerId)
                && Objects.equals(followedId, that.followedId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(followerId, followedId);
    }
}
