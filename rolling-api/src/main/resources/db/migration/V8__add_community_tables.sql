ALTER TABLE users
    ADD COLUMN community_nickname VARCHAR(50);

CREATE TABLE community_posts (
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL,
    category VARCHAR(50) NOT NULL,
    title VARCHAR(80) NOT NULL,
    content TEXT NOT NULL,
    view_count BIGINT NOT NULL DEFAULT 0,
    like_count BIGINT NOT NULL DEFAULT 0,
    comment_count BIGINT NOT NULL DEFAULT 0,
    report_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_community_posts_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_community_posts_author_created_at ON community_posts (author_id, created_at DESC);
CREATE INDEX idx_community_posts_status_created_at ON community_posts (status, created_at DESC);

CREATE TABLE community_comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    report_count BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    deleted_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    CONSTRAINT fk_community_comments_post FOREIGN KEY (post_id) REFERENCES community_posts (id),
    CONSTRAINT fk_community_comments_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE INDEX idx_community_comments_post_created_at ON community_comments (post_id, created_at ASC);
CREATE INDEX idx_community_comments_status_created_at ON community_comments (status, created_at DESC);
