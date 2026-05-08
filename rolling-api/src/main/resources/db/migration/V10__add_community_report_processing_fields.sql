ALTER TABLE community_post_reports
    ADD COLUMN processed_by_user_id BIGINT NULL,
    ADD COLUMN processed_at TIMESTAMP NULL,
    ADD COLUMN processing_memo VARCHAR(1000) NULL,
    ADD COLUMN final_action VARCHAR(100) NULL;

ALTER TABLE community_comment_reports
    ADD COLUMN processed_by_user_id BIGINT NULL,
    ADD COLUMN processed_at TIMESTAMP NULL,
    ADD COLUMN processing_memo VARCHAR(1000) NULL,
    ADD COLUMN final_action VARCHAR(100) NULL;
