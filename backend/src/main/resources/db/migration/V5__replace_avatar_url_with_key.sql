ALTER TABLE users
    RENAME COLUMN avatar_url TO avatar_key;

ALTER TABLE users
    ALTER COLUMN avatar_key TYPE VARCHAR(20);

ALTER TABLE users
    ADD CONSTRAINT ck_users_avatar_key_allowed
    CHECK (
        avatar_key IS NULL
        OR avatar_key IN (
            'AVATAR_01', 'AVATAR_02', 'AVATAR_03', 'AVATAR_04',
            'AVATAR_05', 'AVATAR_06', 'AVATAR_07', 'AVATAR_08'
        )
    );
