-- 작성일시: 2026-06-09 14:51:26 +09:00
CREATE TABLE training_card_likes (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES training_cards (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_training_card_likes_card_user UNIQUE (card_id, user_id)
);

CREATE TABLE training_card_favorites (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES training_cards (id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_training_card_favorites_card_user UNIQUE (card_id, user_id)
);

CREATE TABLE training_log_entry_cards (
    id BIGSERIAL PRIMARY KEY,
    entry_id BIGINT NOT NULL REFERENCES training_log_entries (id) ON DELETE CASCADE,
    card_id BIGINT NOT NULL REFERENCES training_cards (id) ON DELETE CASCADE,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_training_log_entry_cards_entry_card UNIQUE (entry_id, card_id)
);

CREATE INDEX idx_training_card_likes_card_id ON training_card_likes (card_id);
CREATE INDEX idx_training_card_likes_user_id ON training_card_likes (user_id);
CREATE INDEX idx_training_card_favorites_card_id ON training_card_favorites (card_id);
CREATE INDEX idx_training_card_favorites_user_id ON training_card_favorites (user_id);
CREATE INDEX idx_training_log_entry_cards_entry_id ON training_log_entry_cards (entry_id);
CREATE INDEX idx_training_log_entry_cards_card_id ON training_log_entry_cards (card_id);
