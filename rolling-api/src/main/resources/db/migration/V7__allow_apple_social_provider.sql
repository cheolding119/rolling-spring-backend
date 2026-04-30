ALTER TABLE users
    DROP CONSTRAINT IF EXISTS users_social_provider_check;

ALTER TABLE users
    ADD CONSTRAINT users_social_provider_check
        CHECK (social_provider IN ('KAKAO', 'GOOGLE', 'APPLE'));
