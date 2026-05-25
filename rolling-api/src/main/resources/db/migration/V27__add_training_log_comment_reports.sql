ALTER TABLE training_log_comments
    ADD COLUMN report_count BIGINT NOT NULL DEFAULT 0;

CREATE TABLE training_log_comment_reports (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    custom_reason VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    processed_by_user_id BIGINT,
    processed_at TIMESTAMP(6),
    processing_memo VARCHAR(1000),
    final_action VARCHAR(100),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_training_log_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES training_log_comments (id),
    CONSTRAINT fk_training_log_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT uk_training_log_comment_reports_comment_reporter UNIQUE (comment_id, reporter_id),
    CONSTRAINT training_log_comment_reports_reason_check CHECK (reason IN ('FALSE_INFO', 'INAPPROPRIATE', 'SPAM', 'OTHER')),
    CONSTRAINT training_log_comment_reports_status_check CHECK (status IN ('RECEIVED', 'IN_REVIEW', 'RESOLVED', 'REJECTED'))
);

CREATE INDEX idx_training_log_comment_reports_comment
    ON training_log_comment_reports (comment_id);

CREATE INDEX idx_training_log_comment_reports_reporter
    ON training_log_comment_reports (reporter_id);
