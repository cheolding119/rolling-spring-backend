-- 작성일시: 2026-06-09 14:39:13 +09:00
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_cards TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_cards_id_seq TO rolling_admin;
