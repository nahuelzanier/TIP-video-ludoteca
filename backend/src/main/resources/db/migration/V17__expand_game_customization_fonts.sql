ALTER TABLE game_customizations
    DROP CONSTRAINT chk_game_customization_font;

ALTER TABLE game_customizations
    ADD CONSTRAINT chk_game_customization_font
    CHECK (font_family IN (
        'SYSTEM', 'SERIF', 'MONO', 'ARIAL', 'VERDANA',
        'TREBUCHET', 'GEORGIA', 'COURIER'
    ));
