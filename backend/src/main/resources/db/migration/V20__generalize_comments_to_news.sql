ALTER TABLE comments
    ADD COLUMN target_type VARCHAR(10);

UPDATE comments
    SET target_type = 'GAME'
    WHERE target_type IS NULL;

ALTER TABLE comments
    ALTER COLUMN target_type SET NOT NULL;

ALTER TABLE comments
    ADD COLUMN noticia_id BIGINT
        REFERENCES noticias(id) ON DELETE CASCADE;

ALTER TABLE comments
    ALTER COLUMN game_id DROP NOT NULL;

ALTER TABLE comments
    ADD CONSTRAINT chk_comments_target CHECK (
        (target_type = 'GAME' AND game_id IS NOT NULL AND noticia_id IS NULL)
        OR (target_type = 'NEWS' AND game_id IS NULL AND noticia_id IS NOT NULL)
    );

CREATE INDEX idx_comments_noticia_id ON comments(noticia_id);
