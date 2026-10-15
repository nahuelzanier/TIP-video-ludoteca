ALTER TABLE forum_discussions
    ALTER COLUMN cover_image DROP NOT NULL;

ALTER TABLE forum_discussions
    ALTER COLUMN cover_image_content_type DROP NOT NULL;