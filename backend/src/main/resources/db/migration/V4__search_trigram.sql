CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_games_title_trgm ON games USING GIN (upper(title) gin_trgm_ops);
CREATE INDEX idx_app_user_username_trgm ON app_user USING GIN (upper(username) gin_trgm_ops);
