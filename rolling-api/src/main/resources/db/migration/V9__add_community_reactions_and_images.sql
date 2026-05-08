ALTER TABLE community_posts
    ADD COLUMN thumbnail_url VARCHAR(1000);

CREATE TABLE community_post_images (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    image_url VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_community_post_images_post FOREIGN KEY (post_id) REFERENCES community_posts (id)
);

CREATE INDEX idx_community_post_images_post_sort_order ON community_post_images (post_id, sort_order ASC);

CREATE TABLE community_post_likes (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_community_post_likes_post_user UNIQUE (post_id, user_id),
    CONSTRAINT fk_community_post_likes_post FOREIGN KEY (post_id) REFERENCES community_posts (id),
    CONSTRAINT fk_community_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_community_post_likes_post_user ON community_post_likes (post_id, user_id);

CREATE TABLE community_post_reports (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    custom_reason VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_community_post_reports_post_reporter UNIQUE (post_id, reporter_id),
    CONSTRAINT fk_community_post_reports_post FOREIGN KEY (post_id) REFERENCES community_posts (id),
    CONSTRAINT fk_community_post_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
);

CREATE INDEX idx_community_post_reports_post ON community_post_reports (post_id);
CREATE INDEX idx_community_post_reports_reporter ON community_post_reports (reporter_id);

CREATE TABLE community_comment_reports (
    id BIGSERIAL PRIMARY KEY,
    comment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(50) NOT NULL,
    custom_reason VARCHAR(500),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT uq_community_comment_reports_comment_reporter UNIQUE (comment_id, reporter_id),
    CONSTRAINT fk_community_comment_reports_comment FOREIGN KEY (comment_id) REFERENCES community_comments (id),
    CONSTRAINT fk_community_comment_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id)
);

CREATE INDEX idx_community_comment_reports_comment ON community_comment_reports (comment_id);
CREATE INDEX idx_community_comment_reports_reporter ON community_comment_reports (reporter_id);
