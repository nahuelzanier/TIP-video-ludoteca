CREATE TABLE games (
    id VARCHAR(36) PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description TEXT NOT NULL DEFAULT '',
    cover_image_url VARCHAR(500),
    entry_file VARCHAR(500) NOT NULL DEFAULT 'index.html',
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT'
        CHECK (status IN ('DRAFT', 'PUBLISHED')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_games_owner_id ON games(owner_id);
CREATE INDEX idx_games_status ON games(status);

CREATE TABLE game_files (
    game_id VARCHAR(36) NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    path VARCHAR(500) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    content BYTEA NOT NULL,
    PRIMARY KEY (game_id, path)
);