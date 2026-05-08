GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE community_posts TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE community_comments TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE community_post_images TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE community_post_likes TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE community_post_reports TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE community_comment_reports TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE community_posts_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE community_comments_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE community_post_images_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE community_post_likes_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE community_post_reports_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE community_comment_reports_id_seq TO rolling_admin;
