-- 2026-06-11 18:05 KST
CREATE TABLE open_mat_comments (
    id BIGSERIAL PRIMARY KEY,
    open_mat_id BIGINT NOT NULL,
    parent_comment_id BIGINT,
    author_user_id BIGINT NOT NULL,
    content TEXT,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP(6),
    report_count BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_open_mat_comments_open_mat FOREIGN KEY (open_mat_id) REFERENCES open_mats (id),
    CONSTRAINT fk_open_mat_comments_parent FOREIGN KEY (parent_comment_id) REFERENCES open_mat_comments (id),
    CONSTRAINT fk_open_mat_comments_author FOREIGN KEY (author_user_id) REFERENCES users (id)
);

CREATE INDEX idx_open_mat_comments_open_mat_parent_created_at
    ON open_mat_comments (open_mat_id, parent_comment_id, created_at ASC);
