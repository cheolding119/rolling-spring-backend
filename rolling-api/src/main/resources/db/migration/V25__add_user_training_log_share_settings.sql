CREATE TABLE user_training_log_share_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    share_with_friends BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_training_log_share_settings_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_user_training_log_share_settings_user UNIQUE (user_id)
);

CREATE INDEX idx_user_training_log_share_settings_user_id
    ON user_training_log_share_settings (user_id);
