CREATE TABLE game_customizations (
    game_id VARCHAR(36) PRIMARY KEY REFERENCES games(id) ON DELETE CASCADE,
    background_color VARCHAR(7) NOT NULL DEFAULT '#17151f',
    accent_color VARCHAR(7) NOT NULL DEFAULT '#a36aff',
    text_color VARCHAR(7) NOT NULL DEFAULT '#f5f4f8',
    font_family VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    background_style VARCHAR(16) NOT NULL DEFAULT 'SOLID',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_game_customization_background CHECK (background_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_game_customization_accent CHECK (accent_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_game_customization_text CHECK (text_color ~ '^#[0-9A-Fa-f]{6}$'),
    CONSTRAINT chk_game_customization_font CHECK (font_family IN ('SYSTEM', 'SERIF', 'MONO')),
    CONSTRAINT chk_game_customization_background_style CHECK (background_style IN ('SOLID', 'GRADIENT'))
);
