DELETE FROM refresh_tokens rt
USING refresh_tokens dup
WHERE rt.user_id = dup.user_id
  AND rt.id < dup.id;

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uk_refresh_tokens_user_id UNIQUE (user_id);
