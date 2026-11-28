-- Demo data for local presentations. This script never inserts, updates, or deletes games.
-- It uses existing published games for favorites, tags, comments, and forum discussions.
-- Safe to run repeatedly: demo rows are identified by their fixed usernames/titles/content.
-- Demo account password for all demo users: password

DO $$
BEGIN
    IF current_database() <> 'ludarium_dev' THEN
        RAISE EXCEPTION 'This demo seed only runs on ludarium_dev (connected to %).', current_database();
    END IF;
END;
$$;

BEGIN;

INSERT INTO app_user (username, email, password_hash, description, role)
VALUES
    ('demo_ana', 'demo_ana@ludarium.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Fan de los juegos narrativos y las aventuras indie.', 'USER'),
    ('demo_mateo', 'demo_mateo@ludarium.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Me gustan los juegos de estrategia y descubrir proyectos nuevos.', 'USER'),
    ('demo_luna', 'demo_luna@ludarium.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Jugadora casual, fan del pixel art y la música de videojuegos.', 'USER'),
    ('demo_nico', 'demo_nico@ludarium.test', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Siempre buscando un buen desafío.', 'USER')
ON CONFLICT DO NOTHING;

INSERT INTO user_settings (user_id, volume, language, theme)
SELECT id,
       CASE username WHEN 'demo_ana' THEN 70 WHEN 'demo_mateo' THEN 85 WHEN 'demo_luna' THEN 60 ELSE 100 END,
       CASE WHEN username IN ('demo_ana', 'demo_luna') THEN 'es' ELSE 'en' END,
       CASE username WHEN 'demo_ana' THEN 'light' WHEN 'demo_luna' THEN 'dark' ELSE 'system' END
FROM app_user
WHERE username IN ('demo_ana', 'demo_mateo', 'demo_luna', 'demo_nico')
ON CONFLICT (user_id) DO NOTHING;

INSERT INTO follows (follower_id, followed_id)
SELECT follower.id, followed.id
FROM (VALUES
    ('demo_ana', 'demo_mateo'), ('demo_ana', 'demo_luna'),
    ('demo_mateo', 'demo_ana'), ('demo_mateo', 'demo_nico'),
    ('demo_luna', 'demo_ana'), ('demo_luna', 'demo_nico'),
    ('demo_nico', 'demo_mateo'), ('demo_nico', 'demo_luna')
) AS relation(follower_name, followed_name)
JOIN app_user follower ON follower.username = relation.follower_name
JOIN app_user followed ON followed.username = relation.followed_name
ON CONFLICT DO NOTHING;

INSERT INTO noticias (author_id, title, description, published_at, has_image)
SELECT author.id, item.title, item.description,
       CURRENT_TIMESTAMP - item.age,
       FALSE
FROM (VALUES
    ('demo_ana', 'Una aventura que me sorprendió', 'Arranqué pensando que iba a jugar un rato y terminé explorando cada rincón. ¿Qué secretos encontraron ustedes?', INTERVAL '2 days'),
    ('demo_ana', 'Mi recomendación de la semana', 'Si buscan algo para desconectar un rato, denle una oportunidad a este juego. La música y el ritmo me encantaron.', INTERVAL '5 hours'),
    ('demo_mateo', 'Probando una estrategia nueva', 'Cambié mi forma de jugar y por fin superé esa parte que me venía trabando. ¿Qué tácticas les funcionaron?', INTERVAL '1 day'),
    ('demo_mateo', 'Juegos para una partida rápida', 'Estoy armando una lista de juegos para esos ratos libres. ¿Cuál sumarían?', INTERVAL '3 hours'),
    ('demo_luna', 'Detalles que hacen la diferencia', 'Me encanta cuando un juego cuida tanto su arte y sus sonidos. ¿Qué detalle pequeño les llamó la atención?', INTERVAL '18 hours'),
    ('demo_nico', 'Buscando un nuevo desafío', 'Ya terminé mi juego actual y quiero probar algo distinto. Recomienden algo que ponga a prueba los reflejos.', INTERVAL '8 hours')
) AS item(author_name, title, description, age)
JOIN app_user author ON author.username = item.author_name
WHERE NOT EXISTS (
    SELECT 1 FROM noticias existing
    WHERE existing.author_id = author.id AND existing.title = item.title
);

INSERT INTO comments (target_type, noticia_id, author_id, content, rating, created_at)
SELECT 'NEWS', noticia.id, author.id, item.content, NULL,
       noticia.published_at + item.age
FROM (VALUES
    ('Una aventura que me sorprendió', 'demo_mateo', 'Me pasó lo mismo, la exploración está buenísima.', INTERVAL '30 minutes'),
    ('Una aventura que me sorprendió', 'demo_luna', 'La banda sonora también me sorprendió.', INTERVAL '1 hour'),
    ('Mi recomendación de la semana', 'demo_nico', 'Lo agrego a mi lista para probarlo.', INTERVAL '20 minutes'),
    ('Probando una estrategia nueva', 'demo_ana', '¡Bien ahí! A veces cambiar el enfoque ayuda un montón.', INTERVAL '45 minutes'),
    ('Detalles que hacen la diferencia', 'demo_ana', 'Totalmente, el audio le da mucha personalidad.', INTERVAL '1 hour'),
    ('Buscando un nuevo desafío', 'demo_mateo', 'Si te gustan los reflejos, buscá uno de plataformas.', INTERVAL '15 minutes')
) AS item(news_title, author_name, content, age)
JOIN noticias noticia ON noticia.title = item.news_title
JOIN app_user author ON author.username = item.author_name
JOIN app_user owner ON owner.id = noticia.author_id
WHERE owner.username LIKE 'demo_%'
  AND NOT EXISTS (
      SELECT 1 FROM comments existing
      WHERE existing.noticia_id = noticia.id
        AND existing.author_id = author.id
        AND existing.content = item.content
  );

-- The remaining examples are linked only to already published games.
INSERT INTO favorite_games (user_id, game_id)
SELECT app_user.id, game.id
FROM app_user
CROSS JOIN LATERAL (
    SELECT id FROM games WHERE status = 'PUBLISHED' ORDER BY created_at DESC, id LIMIT 3
) game
WHERE app_user.username IN ('demo_ana', 'demo_mateo', 'demo_luna', 'demo_nico')
ON CONFLICT DO NOTHING;

INSERT INTO game_tag_assignments (game_id, tag_id, user_id)
SELECT game.id, tag.id, author.id
FROM (
    SELECT id, row_number() OVER (ORDER BY created_at DESC, id) AS game_number
    FROM games WHERE status = 'PUBLISHED'
    ORDER BY created_at DESC, id LIMIT 4
) game
JOIN tags tag ON tag.slug = CASE game.game_number
    WHEN 1 THEN 'adventure'
    WHEN 2 THEN 'puzzle'
    WHEN 3 THEN 'arcade'
    ELSE 'strategy'
END
JOIN app_user author ON author.username = CASE game.game_number
    WHEN 1 THEN 'demo_ana'
    WHEN 2 THEN 'demo_mateo'
    WHEN 3 THEN 'demo_luna'
    ELSE 'demo_nico'
END
ON CONFLICT DO NOTHING;

INSERT INTO comments (target_type, game_id, author_id, content, rating, created_at)
SELECT 'GAME', game.id, author.id, item.content, item.rating,
       CURRENT_TIMESTAMP - item.age
FROM (
    SELECT id, row_number() OVER (ORDER BY created_at DESC, id) AS game_number
    FROM games WHERE status = 'PUBLISHED'
    ORDER BY created_at DESC, id LIMIT 3
) game
JOIN (VALUES
    (1, 'demo_mateo', 'Me gustó mucho el ritmo. Ideal para jugar una partida más.', 5, INTERVAL '2 days'),
    (1, 'demo_luna', 'El estilo visual tiene mucha personalidad.', 4, INTERVAL '1 day'),
    (2, 'demo_ana', 'Buen desafío, sobre todo cuando empezás a dominar los controles.', 5, INTERVAL '20 hours'),
    (2, 'demo_nico', 'Me costó al principio, pero después no podía parar.', 4, INTERVAL '8 hours'),
    (3, 'demo_ana', 'Una propuesta simple y muy entretenida.', 4, INTERVAL '1 day'),
    (3, 'demo_mateo', '¿Alguien encontró una estrategia para superar el último nivel?', NULL, INTERVAL '3 hours')
) AS item(game_number, author_name, content, rating, age) ON item.game_number = game.game_number
JOIN app_user author ON author.username = item.author_name
WHERE NOT EXISTS (
    SELECT 1 FROM comments existing
    WHERE existing.game_id = game.id
      AND existing.author_id = author.id
      AND existing.content = item.content
);

INSERT INTO forum_discussions (game_id, author_id, title, description, created_at)
SELECT game.id, author.id, item.title, item.description,
       CURRENT_TIMESTAMP - item.age
FROM (VALUES
    (1, 'demo_ana', '¿Cuál fue su momento favorito?', 'Quiero saber qué parte del juego les quedó más grabada. ¿Tuvieron algún momento inesperado o una partida que les salió especialmente bien?', INTERVAL '2 days'),
    (1, 'demo_luna', 'Detalles del apartado visual', 'Hay escenarios y animaciones con mucho encanto. ¿Qué detalle artístico les llamó más la atención?', INTERVAL '1 day'),
    (2, 'demo_mateo', 'Estrategias para el desafío final', 'Estoy buscando distintas formas de encarar el último desafío. ¿Qué estrategia les resultó más efectiva?', INTERVAL '16 hours'),
    (3, 'demo_nico', '¿Qué agregarían al juego?', 'Si pudieran sumar un nivel, una mecánica o un modo nuevo, ¿qué elegirían?', INTERVAL '5 hours')
) AS item(game_number, author_name, title, description, age)
JOIN (
    SELECT id, row_number() OVER (ORDER BY created_at DESC, id) AS game_number
    FROM games WHERE status = 'PUBLISHED'
    ORDER BY created_at DESC, id LIMIT 3
) game ON game.game_number = item.game_number
JOIN app_user author ON author.username = item.author_name
WHERE NOT EXISTS (
    SELECT 1 FROM forum_discussions existing
    WHERE existing.game_id = game.id
      AND existing.author_id = author.id
      AND existing.title = item.title
);

INSERT INTO forum_discussion_forum_tags (discussion_id, forum_tag_id)
SELECT discussion.id, tag.id
FROM forum_discussions discussion
JOIN app_user author ON author.id = discussion.author_id
JOIN forum_tags tag ON tag.slug IN ('discussion', 'gameplay', 'question')
WHERE author.username LIKE 'demo_%'
  AND discussion.title IN (
      '¿Cuál fue su momento favorito?', 'Detalles del apartado visual',
      'Estrategias para el desafío final', '¿Qué agregarían al juego?'
  )
ON CONFLICT DO NOTHING;

INSERT INTO forum_replies (discussion_id, author_id, content, created_at)
SELECT discussion.id, author.id, item.content, discussion.created_at + item.age
FROM (VALUES
    ('¿Cuál fue su momento favorito?', 'demo_mateo', 'La primera vez que encontré un camino secreto. No me lo esperaba.', INTERVAL '30 minutes'),
    ('¿Cuál fue su momento favorito?', 'demo_nico', 'Cuando por fin superé el nivel que me tenía trabado.', INTERVAL '1 hour'),
    ('Detalles del apartado visual', 'demo_ana', 'A mí me gustaron mucho los colores de los escenarios.', INTERVAL '45 minutes'),
    ('Estrategias para el desafío final', 'demo_luna', 'Probá ir con paciencia y aprender el patrón antes de atacar.', INTERVAL '20 minutes'),
    ('¿Qué agregarían al juego?', 'demo_ana', 'Un modo contrarreloj estaría buenísimo.', INTERVAL '35 minutes')
) AS item(discussion_title, author_name, content, age)
JOIN forum_discussions discussion ON discussion.title = item.discussion_title
JOIN app_user creator ON creator.id = discussion.author_id AND creator.username LIKE 'demo_%'
JOIN app_user author ON author.username = item.author_name
WHERE NOT EXISTS (
    SELECT 1 FROM forum_replies existing
    WHERE existing.discussion_id = discussion.id
      AND existing.author_id = author.id
      AND existing.content = item.content
);

INSERT INTO forum_ratings (discussion_id, user_id, rating)
SELECT discussion.id, author.id, 4 + (row_number() OVER (PARTITION BY discussion.id ORDER BY author.username) % 2)::smallint
FROM forum_discussions discussion
JOIN app_user creator ON creator.id = discussion.author_id AND creator.username LIKE 'demo_%'
JOIN app_user author ON author.username IN ('demo_ana', 'demo_mateo', 'demo_luna', 'demo_nico')
WHERE discussion.title IN (
    '¿Cuál fue su momento favorito?', 'Detalles del apartado visual',
    'Estrategias para el desafío final', '¿Qué agregarían al juego?'
)
  AND author.id <> discussion.author_id
ON CONFLICT (discussion_id, user_id) DO NOTHING;

COMMIT;
