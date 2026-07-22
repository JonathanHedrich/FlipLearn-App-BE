ALTER TABLE app_users
    ALTER COLUMN password_hash DROP NOT NULL;

ALTER TABLE app_users
    ADD COLUMN auth_provider VARCHAR(20);

ALTER TABLE app_users
    ADD COLUMN google_subject VARCHAR(255);

ALTER TABLE app_users
    ADD COLUMN email_verified BOOLEAN;

ALTER TABLE app_users
    ADD COLUMN profile_image_url VARCHAR(2048);

UPDATE app_users
SET auth_provider = 'LOCAL'
WHERE auth_provider IS NULL;

UPDATE app_users
SET email_verified = FALSE
WHERE email_verified IS NULL;

ALTER TABLE app_users
    ALTER COLUMN auth_provider SET NOT NULL;

ALTER TABLE app_users
    ALTER COLUMN auth_provider SET DEFAULT 'LOCAL';

ALTER TABLE app_users
    ALTER COLUMN email_verified SET NOT NULL;

ALTER TABLE app_users
    ALTER COLUMN email_verified SET DEFAULT FALSE;

ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_auth_provider
        CHECK (auth_provider IN ('LOCAL', 'GOOGLE'));

ALTER TABLE app_users
    ADD CONSTRAINT chk_app_users_auth_credentials
        CHECK (
            (
                auth_provider = 'LOCAL'
                AND password_hash IS NOT NULL
                AND LENGTH(TRIM(password_hash)) > 0
            )
            OR
            (
                auth_provider = 'GOOGLE'
                AND google_subject IS NOT NULL
                AND LENGTH(TRIM(google_subject)) > 0
            )
        );

CREATE UNIQUE INDEX ux_app_users_google_subject
    ON app_users (google_subject)
    WHERE google_subject IS NOT NULL;