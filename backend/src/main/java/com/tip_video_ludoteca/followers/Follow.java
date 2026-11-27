package com.tip_video_ludoteca.followers;

import com.tip_video_ludoteca.users.User;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "follows")
public class Follow {

    @EmbeddedId
    private FollowId id;

    @MapsId("followerId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "follower_id", nullable = false)
    private User follower;

    @MapsId("followedId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "followed_id", nullable = false)
    private User followed;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected Follow() {
    }

    public Follow(User follower, User followed) {
        this.id = new FollowId(follower.getId(), followed.getId());
        this.follower = follower;
        this.followed = followed;
    }

    public FollowId getId() {
        return id;
    }

    public User getFollower() {
        return follower;
    }

    public User getFollowed() {
        return followed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
