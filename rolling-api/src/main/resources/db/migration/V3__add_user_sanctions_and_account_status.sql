ALTER TABLE users
    ADD COLUMN account_status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE';

ALTER TABLE users
    ADD COLUMN suspension_until TIMESTAMP(6);

ALTER TABLE users
    ADD COLUMN sanction_reason_summary VARCHAR(255);

UPDATE users
SET account_status = 'WITHDRAWN',
    suspension_until = NULL,
    sanction_reason_summary = NULL
WHERE is_withdrawn = TRUE;

CREATE TABLE user_sanctions (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sanction_type VARCHAR(50) NOT NULL,
    reason VARCHAR(255) NOT NULL,
    memo VARCHAR(1000),
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6),
    created_by_user_id BIGINT NOT NULL,
    released_by_user_id BIGINT,
    released_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_user_sanctions_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_sanctions_created_by FOREIGN KEY (created_by_user_id) REFERENCES users (id),
    CONSTRAINT fk_user_sanctions_released_by FOREIGN KEY (released_by_user_id) REFERENCES users (id)
);

CREATE INDEX idx_user_sanctions_user_created_at ON user_sanctions (user_id, created_at DESC);
CREATE INDEX idx_user_sanctions_user_released_at ON user_sanctions (user_id, released_at);
