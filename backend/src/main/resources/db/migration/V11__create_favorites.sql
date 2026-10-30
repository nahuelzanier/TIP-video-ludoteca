CREATE TABLE favorite_games (
    user_id BIGINT NOT NULL
        REFERENCES app_user(id) ON DELETE CASCADE,
    game_id VARCHAR(36) NOT NULL
        REFERENCES games(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_favorite_games PRIMARY KEY (user_id, game_id)
);

CREATE INDEX idx_favorite_games_user_id
    ON favorite_games(user_id, created_at DESC);

CREATE INDEX idx_favorite_games_game_id
    ON favorite_games(game_id);
