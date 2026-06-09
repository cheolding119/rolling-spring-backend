-- 작성일시: 2026-06-09 14:51:26 +09:00
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_card_likes TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_card_favorites TO rolling_admin;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_log_entry_cards TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_card_likes_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_card_favorites_id_seq TO rolling_admin;
GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_log_entry_cards_id_seq TO rolling_admin;
