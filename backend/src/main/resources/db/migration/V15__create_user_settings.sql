CREATE TABLE user_settings (
    user_id BIGINT PRIMARY KEY
        REFERENCES app_user(id) ON DELETE CASCADE,
    volume SMALLINT NOT NULL DEFAULT 100
        CHECK (volume BETWEEN 0 AND 100),
    language VARCHAR(5) NOT NULL DEFAULT 'en'
        CHECK (language IN ('en', 'es')),
    theme VARCHAR(10) NOT NULL DEFAULT 'system'
        CHECK (theme IN ('system', 'light', 'dark'))
);
