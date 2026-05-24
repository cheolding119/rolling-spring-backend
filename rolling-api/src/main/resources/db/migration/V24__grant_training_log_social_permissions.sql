GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE friend_requests TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE user_friends TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_log_likes TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_log_comments TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE friend_requests_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE user_friends_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_log_likes_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_log_comments_id_seq TO rolling_admin;
