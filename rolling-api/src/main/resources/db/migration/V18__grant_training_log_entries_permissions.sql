GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE training_log_entries TO rolling_admin;

GRANT USAGE, SELECT, UPDATE ON SEQUENCE training_log_entries_id_seq TO rolling_admin;
