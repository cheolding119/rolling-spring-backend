-- 2026-06-11 18:06 KST
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE open_mat_comments TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE open_mat_comments_id_seq TO rolling_admin;
