ALTER TABLE user_profiles
    DROP INDEX uk_user_profiles_toss_user_key;

ALTER TABLE user_profiles
    ADD COLUMN user_id BIGINT NULL AFTER user_profile_id;

UPDATE user_profiles up
    JOIN users u ON up.toss_user_key = u.toss_user_key
SET up.user_id = u.user_id;

ALTER TABLE user_profiles
    MODIFY user_id BIGINT NOT NULL;

ALTER TABLE user_profiles
    DROP COLUMN toss_user_key;

ALTER TABLE user_profiles
    MODIFY user_profile_id BIGINT NOT NULL;

ALTER TABLE user_profiles
    DROP PRIMARY KEY;

ALTER TABLE user_profiles
    DROP COLUMN user_profile_id;

ALTER TABLE user_profiles
    ADD PRIMARY KEY (user_id);

ALTER TABLE user_profiles
    ADD CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id)
            REFERENCES users(user_id)
            ON DELETE CASCADE;