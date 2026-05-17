CREATE TABLE training_log_entries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    training_date DATE NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    checklist_json TEXT,
    hashtags_json TEXT,
    image_url VARCHAR(1000),
    external_links_json TEXT,
    training_minutes INTEGER,
    belt_color VARCHAR(50),
    stripe_count INTEGER,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_training_log_entries_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT training_log_entries_category_check CHECK (
        category IN ('TECHNIQUE', 'SPARRING', 'TOURNAMENT', 'PROMOTION', 'OPEN_MAT', 'DRILL', 'PERSONAL_TRAINING')
    ),
    CONSTRAINT training_log_entries_training_minutes_check CHECK (
        training_minutes IS NULL OR (training_minutes >= 0 AND training_minutes <= 600)
    ),
    CONSTRAINT training_log_entries_stripe_count_check CHECK (
        stripe_count IS NULL OR stripe_count >= 0
    )
);

CREATE INDEX idx_training_log_entries_user_date ON training_log_entries (user_id, training_date);
CREATE INDEX idx_training_log_entries_user_created_at ON training_log_entries (user_id, created_at);
