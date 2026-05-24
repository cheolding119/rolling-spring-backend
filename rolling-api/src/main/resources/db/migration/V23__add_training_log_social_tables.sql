ALTER TABLE training_log_entries
    ADD COLUMN visibility VARCHAR(20) NOT NULL DEFAULT 'PRIVATE';

ALTER TABLE training_log_entries
    ADD CONSTRAINT training_log_entries_visibility_check
        CHECK (visibility IN ('PRIVATE', 'FRIENDS'));

CREATE TABLE friend_requests (
    id BIGSERIAL PRIMARY KEY,
    sender_user_id BIGINT NOT NULL,
    receiver_user_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    responded_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_friend_requests_sender FOREIGN KEY (sender_user_id) REFERENCES users (id),
    CONSTRAINT fk_friend_requests_receiver FOREIGN KEY (receiver_user_id) REFERENCES users (id),
    CONSTRAINT friend_requests_status_check CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'CANCELED')),
    CONSTRAINT friend_requests_not_self_check CHECK (sender_user_id <> receiver_user_id)
);

CREATE INDEX idx_friend_requests_sender_status_created_at
    ON friend_requests (sender_user_id, status, created_at DESC);

CREATE INDEX idx_friend_requests_receiver_status_created_at
    ON friend_requests (receiver_user_id, status, created_at DESC);

CREATE TABLE user_friends (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    friend_user_id BIGINT NOT NULL,
    friended_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_friends_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_friends_friend_user FOREIGN KEY (friend_user_id) REFERENCES users (id),
    CONSTRAINT user_friends_not_self_check CHECK (user_id <> friend_user_id),
    CONSTRAINT uk_user_friends_user_friend UNIQUE (user_id, friend_user_id)
);

CREATE INDEX idx_user_friends_user_friended_at
    ON user_friends (user_id, friended_at DESC);

CREATE TABLE training_log_likes (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_training_log_likes_entry FOREIGN KEY (entry_id) REFERENCES training_log_entries (id),
    CONSTRAINT fk_training_log_likes_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_training_log_likes_entry_user UNIQUE (entry_id, user_id)
);

CREATE INDEX idx_training_log_likes_entry_id
    ON training_log_likes (entry_id);

CREATE TABLE training_log_comments (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    author_user_id BIGINT NOT NULL,
    content TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_training_log_comments_entry FOREIGN KEY (entry_id) REFERENCES training_log_entries (id),
    CONSTRAINT fk_training_log_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES training_log_comments (id),
    CONSTRAINT fk_training_log_comments_author FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE INDEX idx_training_log_comments_entry_parent_created_at
    ON training_log_comments (entry_id, parent_comment_id, created_at ASC);
