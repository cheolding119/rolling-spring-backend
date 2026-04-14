ALTER TABLE user_blocked_users
    ADD COLUMN blocked_at TIMESTAMP(6);

UPDATE user_blocked_users
SET blocked_at = CURRENT_TIMESTAMP
WHERE blocked_at IS NULL;

ALTER TABLE user_blocked_users
    ALTER COLUMN blocked_at SET NOT NULL;
