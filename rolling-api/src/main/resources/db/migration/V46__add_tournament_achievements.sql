-- 2026-10-03 15:00 KST
CREATE TABLE tournament_achievements (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    tournament_name VARCHAR(255) NOT NULL,
    competition_date DATE NOT NULL,
    result VARCHAR(30) NOT NULL CHECK (result IN ('GOLD', 'SILVER', 'BRONZE', 'PARTICIPATION')),
    uniform_type VARCHAR(30) CHECK (uniform_type IN ('GI', 'NO_GI')),
    belt_color VARCHAR(30) CHECK (belt_color IN (
        'WHITE', 'GRAY_WHITE', 'GRAY', 'GRAY_BLACK', 'YELLOW_WHITE', 'YELLOW', 'YELLOW_BLACK',
        'ORANGE_WHITE', 'ORANGE', 'ORANGE_BLACK', 'GREEN_WHITE', 'GREEN', 'GREEN_BLACK',
        'BLUE', 'PURPLE', 'BROWN', 'BLACK'
    )),
    age_division VARCHAR(100),
    division_type VARCHAR(30) CHECK (division_type IN ('WEIGHT', 'ABSOLUTE', 'OTHER')),
    weight_class VARCHAR(100),
    organizer VARCHAR(255),
    result_url VARCHAR(1000),
    memo VARCHAR(500),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT tournament_achievements_absolute_weight_check
        CHECK (division_type <> 'ABSOLUTE' OR weight_class IS NULL)
);

CREATE INDEX idx_tournament_achievements_user_date
    ON tournament_achievements (user_id, competition_date DESC, id DESC);
