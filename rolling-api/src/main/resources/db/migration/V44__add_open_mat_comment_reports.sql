-- 2026-06-11 21:20 KST
CREATE TABLE open_mat_comment_reports (
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
    CONSTRAINT fk_open_mat_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES open_mat_comments (id),
    CONSTRAINT fk_open_mat_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id),
    CONSTRAINT uq_open_mat_comment_reports_comment_reporter UNIQUE (comment_id, reporter_id)
);

CREATE INDEX idx_open_mat_comment_reports_comment
    ON open_mat_comment_reports (comment_id);

CREATE INDEX idx_open_mat_comment_reports_reporter
    ON open_mat_comment_reports (reporter_id);

CREATE INDEX idx_open_mat_comment_reports_status_created_at
    ON open_mat_comment_reports (status, created_at DESC, id DESC);
