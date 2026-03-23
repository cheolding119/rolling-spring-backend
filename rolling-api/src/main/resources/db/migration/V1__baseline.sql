CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    nickname VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(255),
    social_provider VARCHAR(255) NOT NULL,
    social_id VARCHAR(255) NOT NULL,
    belt_color VARCHAR(255) NOT NULL,
    is_withdrawn BOOLEAN NOT NULL DEFAULT FALSE,
    withdrawal_pending BOOLEAN NOT NULL DEFAULT FALSE,
    withdrawal_requested_at TIMESTAMP(6),
    withdrawal_scheduled_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_users_social_id_provider UNIQUE (social_id, social_provider)
);

CREATE TABLE user_devices (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(512) NOT NULL,
    platform VARCHAR(30),
    device_id VARCHAR(255),
    app_version VARCHAR(50),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_user_devices_fcm_token UNIQUE (fcm_token),
    CONSTRAINT fk_user_devices_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE user_blocked_users (
    user_id BIGINT NOT NULL,
    blocked_user_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, blocked_user_id),
    CONSTRAINT fk_user_blocked_users_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_blocked_users_blocked_user FOREIGN KEY (blocked_user_id) REFERENCES users (id)
);

CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(512) NOT NULL,
    user_id BIGINT NOT NULL,
    expiry_date TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_refresh_tokens_token UNIQUE (token)
);

CREATE TABLE open_mats (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    start_date_time TIMESTAMP(6) NOT NULL,
    end_date_time TIMESTAMP(6) NOT NULL,
    location_name VARCHAR(255),
    address VARCHAR(255),
    region VARCHAR(255) NOT NULL,
    max_capacity INTEGER NOT NULL DEFAULT -1,
    status VARCHAR(255) NOT NULL DEFAULT 'RECRUITING',
    report_count INTEGER NOT NULL DEFAULT 0,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    manual_closed BOOLEAN NOT NULL DEFAULT FALSE,
    host_instagram_id VARCHAR(255),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_open_mats_host FOREIGN KEY (host_id) REFERENCES users (id)
);

CREATE TABLE open_mat_participants (
    open_mat_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (open_mat_id, user_id),
    CONSTRAINT fk_open_mat_participants_open_mat FOREIGN KEY (open_mat_id) REFERENCES open_mats (id)
);

CREATE TABLE tournaments (
    id BIGSERIAL PRIMARY KEY,
    host_user_id BIGINT,
    source VARCHAR(50),
    title VARCHAR(255) NOT NULL,
    organizer VARCHAR(255),
    poster_url VARCHAR(1000),
    competition_date VARCHAR(255) NOT NULL,
    registration_deadline VARCHAR(255),
    location VARCHAR(255),
    apply_link VARCHAR(512) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_tournaments_apply_link UNIQUE (apply_link),
    CONSTRAINT uk_tournaments_title_competition_date UNIQUE (title, competition_date)
);

CREATE INDEX idx_tournaments_competition_date ON tournaments (competition_date);

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    target_type VARCHAR(255) NOT NULL,
    target_id BIGINT NOT NULL,
    reason VARCHAR(255) NOT NULL,
    custom_reason VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    processed_by_user_id BIGINT,
    processed_at TIMESTAMP(6),
    processing_memo VARCHAR(1000),
    final_action VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uk_reports_reporter_target UNIQUE (reporter_id, target_type, target_id),
    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
);

CREATE INDEX idx_reports_target ON reports (target_type, target_id);
CREATE INDEX idx_reports_status_created_at ON reports (status, created_at);

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    target_id BIGINT NOT NULL,
    route VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body VARCHAR(500) NOT NULL,
    read_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT notifications_type_check CHECK (type IN ('OPEN_MAT_UPDATED', 'OPEN_MAT_DELETED', 'INQUIRY_ANSWERED'))
);

CREATE INDEX idx_notifications_user_created_at ON notifications (user_id, created_at);

CREATE TABLE notices (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    author_name VARCHAR(100) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL
);

CREATE INDEX idx_notices_created_at ON notices (created_at);

CREATE TABLE inquiries (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    answer_content TEXT,
    answered_by_user_id BIGINT,
    answered_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_inquiries_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_inquiries_user_created_at ON inquiries (user_id, created_at);
CREATE INDEX idx_inquiries_status_created_at ON inquiries (status, created_at);
