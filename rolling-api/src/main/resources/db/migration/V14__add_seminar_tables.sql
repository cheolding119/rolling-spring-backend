CREATE TABLE seminars (
    id BIGSERIAL PRIMARY KEY,
    host_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    main_image_url VARCHAR(1000),
    instructor_name VARCHAR(255) NOT NULL,
    instructor_bio TEXT,
    curriculum TEXT,
    target_audience TEXT,
    preparation TEXT,
    contact_info VARCHAR(255),
    host_instagram_id VARCHAR(255),
    start_date_time TIMESTAMP(6) NOT NULL,
    end_date_time TIMESTAMP(6) NOT NULL,
    application_start_date_time TIMESTAMP(6),
    application_end_date_time TIMESTAMP(6),
    location_name VARCHAR(255) NOT NULL,
    address VARCHAR(255) NOT NULL,
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    region VARCHAR(255) NOT NULL,
    max_capacity INTEGER NOT NULL DEFAULT -1,
    price INTEGER NOT NULL DEFAULT 0,
    payment_guide TEXT,
    refund_policy TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'RECRUITING',
    report_count INTEGER NOT NULL DEFAULT 0,
    is_hidden BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_seminars_host FOREIGN KEY (host_id) REFERENCES users (id),
    CONSTRAINT seminars_status_check CHECK (status IN ('RECRUITING', 'CLOSED', 'CANCELED', 'FINISHED', 'DELETED')),
    CONSTRAINT seminars_capacity_check CHECK (max_capacity = -1 OR max_capacity >= 1),
    CONSTRAINT seminars_price_check CHECK (price >= 0),
    CONSTRAINT seminars_coordinate_pair_check CHECK (
        (latitude IS NULL AND longitude IS NULL)
        OR (latitude IS NOT NULL AND longitude IS NOT NULL)
    )
);

CREATE INDEX idx_seminars_status_start ON seminars (status, start_date_time);
CREATE INDEX idx_seminars_region_start ON seminars (region, start_date_time);
CREATE INDEX idx_seminars_host ON seminars (host_id);

CREATE TABLE seminar_applications (
    id BIGSERIAL PRIMARY KEY,
    seminar_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'APPLIED',
    cancel_reason VARCHAR(500),
    applied_at TIMESTAMP(6) NOT NULL,
    canceled_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_seminar_applications_seminar FOREIGN KEY (seminar_id) REFERENCES seminars (id),
    CONSTRAINT fk_seminar_applications_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT uk_seminar_applications_seminar_user UNIQUE (seminar_id, user_id),
    CONSTRAINT seminar_applications_status_check CHECK (status IN ('APPLIED', 'CANCELED', 'HOST_CANCELED', 'SEMINAR_CANCELED', 'ATTENDED', 'NO_SHOW'))
);

CREATE INDEX idx_seminar_applications_user_status ON seminar_applications (user_id, status);
CREATE INDEX idx_seminar_applications_seminar_status ON seminar_applications (seminar_id, status);
