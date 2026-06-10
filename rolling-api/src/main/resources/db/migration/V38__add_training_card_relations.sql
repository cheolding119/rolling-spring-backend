-- 작성일시: 2026-06-10 11:05:00 +09:00
CREATE TABLE training_card_relations (
    id BIGSERIAL PRIMARY KEY,
    card_id BIGINT NOT NULL REFERENCES training_cards (id) ON DELETE CASCADE,
    related_card_id BIGINT NOT NULL REFERENCES training_cards (id) ON DELETE CASCADE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_training_card_relations_card_related UNIQUE (card_id, related_card_id),
    CONSTRAINT chk_training_card_relations_not_self CHECK (card_id <> related_card_id)
);

CREATE INDEX idx_training_card_relations_card_id ON training_card_relations (card_id);
CREATE INDEX idx_training_card_relations_related_card_id ON training_card_relations (related_card_id);
