ALTER TABLE app_user
    ADD COLUMN profile_image BYTEA,
    ADD COLUMN profile_image_content_type VARCHAR(50);

ALTER TABLE app_user
    DROP COLUMN profile_image_url;