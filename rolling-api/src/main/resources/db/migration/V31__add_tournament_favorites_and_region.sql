-- 2026-06-01 15:30 Asia/Seoul

ALTER TABLE tournaments
    ADD COLUMN region VARCHAR(50);

CREATE TABLE tournament_favorites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    tournament_id BIGINT NOT NULL,
    notification_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    remind_date DATE,
    remind_time TIME,
    scheduled_at TIMESTAMP(6),
    sent_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_tournament_favorites_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_tournament_favorites_tournament FOREIGN KEY (tournament_id) REFERENCES tournaments (id),
    CONSTRAINT uk_tournament_favorites_user_tournament UNIQUE (user_id, tournament_id)
);

CREATE INDEX idx_tournament_favorites_user_created_at
    ON tournament_favorites (user_id, created_at);

CREATE INDEX idx_tournament_favorites_notification_schedule
    ON tournament_favorites (notification_enabled, scheduled_at, sent_at);
