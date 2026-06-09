-- 작성일시: 2026-06-09 14:39:13 +09:00
CREATE TABLE training_cards (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    topic VARCHAR(100) NOT NULL,
    level VARCHAR(30) NOT NULL,
    position VARCHAR(30) NOT NULL,
    situation_summary VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    situation_description TEXT NOT NULL,
    starting_position_description TEXT NOT NULL,
    flow_description TEXT NOT NULL,
    key_points TEXT NOT NULL,
    common_mistakes TEXT NOT NULL,
    cautions TEXT NOT NULL,
    youtube_url VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT training_cards_level_check CHECK (
        level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED')
    ),
    CONSTRAINT training_cards_position_check CHECK (
        position IN ('STANDING', 'GUARD')
    ),
    CONSTRAINT training_cards_display_order_check CHECK (
        display_order >= 0
    )
);

CREATE INDEX idx_training_cards_active_display_order
    ON training_cards (active, display_order);

CREATE INDEX idx_training_cards_level_position
    ON training_cards (level, position);
