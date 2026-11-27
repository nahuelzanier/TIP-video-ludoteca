CREATE TABLE follows (
    follower_id BIGINT NOT NULL
        REFERENCES app_user(id) ON DELETE CASCADE,
    followed_id BIGINT NOT NULL
        REFERENCES app_user(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_follows PRIMARY KEY (follower_id, followed_id),
    CONSTRAINT chk_follows_not_self CHECK (follower_id <> followed_id)
);

CREATE INDEX idx_follows_follower_id
    ON follows(follower_id, created_at DESC);

CREATE INDEX idx_follows_followed_id
    ON follows(followed_id);
