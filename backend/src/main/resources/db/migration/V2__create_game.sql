CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE game (
    id VARCHAR(50) PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    image VARCHAR(255) NOT NULL,
    description VARCHAR(500) NOT NULL
);

INSERT INTO game (id, title, image, description) VALUES
    ('game1', 'Blasteroids', '/images/game1.png', 'Un clon de Asteroids.'),
    ('game2', 'La Casa Esta Llena', '/images/game2.png', 'Algo extraño.'),
    ('game3', 'LayLand', '/images/game3.png', 'Una aventura de puzzles.');

CREATE INDEX idx_game_title_trgm ON game USING GIN (upper(title) gin_trgm_ops);
CREATE INDEX idx_app_user_username_trgm ON app_user USING GIN (upper(username) gin_trgm_ops);
